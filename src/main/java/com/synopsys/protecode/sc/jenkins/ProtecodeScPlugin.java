/*******************************************************************************
 * Copyright (c) 2017 Synopsys, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Synopsys, Inc - initial implementation and documentation
 *******************************************************************************/

package com.synopsys.protecode.sc.jenkins;


import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import com.synopsys.protecode.sc.jenkins.interfaces.Listeners;
import com.synopsys.protecode.sc.jenkins.interfaces.Listeners.PollService;
import com.synopsys.protecode.sc.jenkins.interfaces.Listeners.ResultService;
import com.synopsys.protecode.sc.jenkins.interfaces.Listeners.ScanService;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.ScanResultResponse;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.UploadResponse;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes.FileAndResult;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import okhttp3.MediaType;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * TODO: There are much too many variables stored on the object level. Maybe we could perhaps store them
 * in a configuration object or much more preferably as temp variables being moved in the methods. 
 * This would eliminate the danger of having a variable instantiated to an old value from an older 
 * build.
 */
public class ProtecodeScPlugin extends Builder implements SimpleBuildStep {  
  
  private String credentialsId;  
  private String protecodeScGroup; // TODO: Group can be an integer
  private String directoryToScan;
  private boolean includeSubdirectories;
  private String pattern; // Be carefull with this. 
  private boolean convertToSummary;
  private boolean failIfVulns;
  private int scanTimeout;
  
  // don't access service directly, use service(). It checks whether this exists
  private ProtecodeScService service = null;
  
  // used to know whether to make a new service
  private static URL storedHost = null;
  private static boolean storedDontCheckCertificate = true;
  
  // Used in the scan process
  private List<FileAndResult> results;
  private long stopAt = 0;
  
  // used for printing to the jenkins console
  private PrintStream log = null;
  private TaskListener listener = null;
  
  public static final String REPORT_DIRECTORY = "reports";
  public static final String NO_ERROR = ""; // TODO: Use Optional
  
  private static final Logger LOGGER = Logger.getLogger(ProtecodeScPlugin.class.getName());
  
  //private static Optional<Boolean> lastBuildSucces;
  
  @DataBoundConstructor
  public ProtecodeScPlugin(
    String credentialsId,
    String protecodeScGroup     
  ) {
    LOGGER.warning("BUILDING NEW PLUGIN");
    this.credentialsId = credentialsId;
    this.protecodeScGroup = protecodeScGroup;
    this.includeSubdirectories = false;
    this.directoryToScan = "";
    this.pattern = "";
    this.convertToSummary = false;
    this.failIfVulns = true;
    this.scanTimeout = 10;
  }  
    
  private ProtecodeScService service() {
    // TODO: Add check that service is ok. We might need to do a dummy call to the server for it.
    getDescriptor().load();
    
    try {
      if (service == null
        // We need to check whether we need a new instance of the backend.
        || !getDescriptor().getProtecodeScHost().equals(storedHost.toExternalForm())
        || getDescriptor().isDontCheckCert() != storedDontCheckCertificate) {       
        storedHost = new URL(getDescriptor().getProtecodeScHost());        
        storedDontCheckCertificate = getDescriptor().isDontCheckCert();        

        service = new ProtecodeScService(
          credentialsId,
          storedHost,
          !getDescriptor().isDontCheckCert()
        );      
      }
    } catch (Exception e){
      listener.error("Cannot read Protecode URL, please make sure it has been set in the Jenkins"
          + " configuration page.");
    }    
    return service;
  }
  
  // TODO rewrite timer logic, this isn't good
  private boolean isTimeout() {
    return System.currentTimeMillis() > stopAt;
  }
  
  private void startPollTimer() {
    // stopAt is set to be the moment we don't try to poll anymore
    // TODO: For a more comprehensive timeout, this isn't enough for the whole plugin.
    // For build timeouts, suggest to use https://plugins.jenkins.io/build-timeout
    stopAt = System.currentTimeMillis() + 1000L * 60 * scanTimeout;
  }
  
  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
    throws InterruptedException, IOException
  { 
    this.listener = listener;
    doPerform(run, workspace);
  }
  
  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
    BuildListener listener) throws InterruptedException, IOException
  {
    this.listener = listener;
    return doPerform(build, build.getWorkspace());
  }
  
  public boolean doPerform(Run<?, ?> run, FilePath workspace)
    throws IOException, InterruptedException
  {
    log = listener.getLogger();
    log.println("/---------- Protecode SC plugin start ----------/");       
    
    // use shortened word to distinguish from possibly null service
    ProtecodeScService serv = service();
    if (serv == null) {
      listener.error("Cannot connect to Protecode SC");
      return false;
    }
      
    if (!UtilitiesGeneral.connectionOk(service.connectionOk())) {
      listener.fatalError("Problem with connecting to Protecode SC, exiting");
      return false;
    }
    
    // TODO: Make a nice structured printing of build variables and other information to the 
    // console. Right now all printing is distributed everyhere and it causes confusion.
    if (failIfVulns) {
      log.println("The build will fail if any vulnurabilities are found.");
    } else {
      log.println("The build will NOT fail if vulnurabilities are found.");
    }
    
    results = new ArrayList<>(); // clean array for use.
    
    @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    String directoryToScan = (null != getDirectoryToScan()) ? getDirectoryToScan() : "";     
   
    if (includeSubdirectories){ 
      log.println("Including subdirectories");
    }
    
    @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    List<ReadableFile> filesToScan = UtilitiesFile.getFiles(directoryToScan,
      workspace,      
      includeSubdirectories,
      UtilitiesFile.patternOrAll(pattern),
      run,
      listener
    );
    
    log.println("Sending following files to Protecode SC:");
    filesToScan.forEach((ReadableFile file) -> 
      (log.println(file)));
    
    if (filesToScan.isEmpty()) {
      // no files to scan, no failure
      log.println("No files to scan found. Protecode SC plugin exits without failing.");      
      return true;
    }    
    
    long start = System.currentTimeMillis();
    
    for (ReadableFile file: filesToScan) {
      LOGGER.log(Level.FINE, "Sending file: {0}", file.name());
      serv.scan(
        protecodeScGroup,
        file.name(),
        new StreamRequestBody
        (
          MediaType.parse("application/octet-stream"),
          file
        ),
        new ScanService() {
          @Override
          public void processUploadResult(UploadResponse result) {
            addUploadResponse(log, file.name(), result, NO_ERROR);
          }
          @Override
          public void setError(String reason) {
            // TODO: use Optional
            log.println(reason);
            // TODO: Maybe use listener.error to stop writing for more results if we get error 
            // perhaps?
            addUploadResponse(log, file.name(), null, reason);
          }
        }
      );
      Thread.sleep(500); // we don't want to overload anything
    }
    
    // Then we wait and continue only when we have as many UploadResponses as we have
    // filesToScan. Sad but true
    log.println("Polling Protecode SC for scan results");
    waitForUploadResponses(filesToScan.size(), log);
    
    long time = (System.currentTimeMillis() - start)/1000;
    LOGGER.log(Level.INFO, "Uploading files to protecode sc took: {0} seconds", time);
    
    // start polling for reponses to scans
    if (!poll()) {
      // maybe we were interrupted or something failed, ending phase
      return false;
    }
    
    //evaluate, if verdict is false, there are vulns
    boolean verdict = ProtecodeEvaluator.evaluate(results);
    
    // make results
    ReportBuilder.report(results, listener, REPORT_DIRECTORY, workspace);
    
    // summarise
    if(convertToSummary) {
      ReportBuilder.makeSummary(results, run, listener, REPORT_DIRECTORY, workspace);
    }
    log.println("/---------- Protecode SC plugin end -----------/");
    if (failIfVulns) {  
      // TODO: Fail explicitly
//      if (!verdict) {
//        listener.fatalError("Vulnerabilities found.");
//      }
      return verdict;
    } else {
      return true;
    }
  }
  
  /**
   * Called by the lamdas given to upload rest calls
   * @param response The responses fetched from Protecode SC
   */
  private void addUploadResponse(PrintStream log, String name, UploadResponse response, String error) {
    if (NO_ERROR.equals(error)) {
      results.add(new FileAndResult(name, response));
    } else {
      // TODO, if en error which will stop the build from happening we should stop the build.     
      results.add(new FileAndResult(name, error));
    }
  }
  
  /**
   * TODO clean up depth, move logic to other methods.
   * @param listener
   */
  private boolean poll() {
    if (results.stream().allMatch((fileAndResult) -> (fileAndResult.hasError()))) {
      log.println("No results found. Perhaps no uploads were succesfull.");
      return false;
    }
    startPollTimer();
    // use shortened word to distinguish from possibly null service    
    ProtecodeScService serv = service();
    log.println("Fetching results from Protecode SC");
    do {
      if (isTimeout()) {
        return false;
      }
      results.forEach((FileAndResult fileAndResult) -> {
        if (!fileAndResult.hasError()) { // if we got an error from file upload        
          // TODO: Add check if the result was never reached
          if (!fileAndResult.hasScanResponse()) {  // if this return true, we can ignore the fileAndResult
            if (fileAndResult.uploadHTTPStatus() == 200) {
              if ("R".equals(fileAndResult.getState())) {
                if (!fileAndResult.isResultBeingFetched()) {
                  fileAndResult.setResultBeingFetched(true);
                  serv.scanResult(
                    fileAndResult.getUploadResponse().getResults().getSha1sum(),
                    new ResultService() {
                      @Override
                      public void setScanResult(ScanResultResponse result) {
                        fileAndResult.setResultResponse(result);
                      }

                      @Override
                      public void setError(String reason) {
                        log.println("Received Protecode SC scan result ERROR for file: " + fileAndResult.getFilename());
                        fileAndResult.setError(reason);
                      }
                    }
                  );
                }
              } else {
                serv.poll(
                  // TODO: Use pretty annotation in type "product_id"
                  fileAndResult.getUploadResponse().getResults().getProduct_id(),
                  new PollService() {
                    @Override
                    public void setScanStatus(UploadResponse status) {
                      fileAndResult.setUploadResponse(status);
                    }

                    @Override
                    public void setError(String reason) {
                      log.println("scan status ERROR: " + fileAndResult.getFilename() + ": " + fileAndResult.getState() + ": " + reason);
                      fileAndResult.setError(reason);
                    }
                  }
                );
              }
            } else {
              listener.error("Status code for file upload: '" + fileAndResult.getFilename() +
                "' was " + fileAndResult.uploadHTTPStatus() + ". No results to fetch.");
            }
          }
        }
        try {
          Thread.sleep(500); // we don't want to overload the network with bulk requests
        } catch (InterruptedException ex) {
          // Do nothing. Maybe the build has been canceled.
        }
      });
      
      if (allNotReady()) {
        try {
          Thread.sleep(15 * 1000);
        } catch (InterruptedException e) {
          return false;
        }
      }
    } while (allNotReady());
    log.println("Received all results from Protecode SC");
    return true;
  }
  
  private boolean allNotReady() {
    return results.stream().anyMatch((fileAndResult) -> (!fileAndResult.hasScanResponse()));
  }
  
  /**
   * Waits until all upload results are in. Returns only then.
   * @param fileCount How many files were uploaded
   * @param log for printing to Jenkins build console.
   */
  private void waitForUploadResponses(int fileCount, PrintStream log) {
    boolean waitForResponses = true;
    // TODO: Add timeout since some files get no reponse from protecode
    while (waitForResponses) {
      try {
        Thread.sleep(30 * 1000);
        // TODO: remove print after testing
        if (results.size() >= fileCount) {
          waitForResponses = false;
        }
      } catch (InterruptedException ie) {
        waitForResponses = false;
        log.println("Interrupted while waiting for upload responses from Protecode SC");
      }
    }
  }
  
// TODO: move file getting/handling to own method in this or utils
//  private List<ReadableFile> getFiles() {
//    @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
//    String directoryToScan = (null != getFilesToScanDirectory()) ? getFilesToScanDirectory() : "";
//    
//    
//    if (includeSubdirectories){ 
//      log.println("Including subdirectories");
//    }
//    
//    @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
//    return UtilitiesFile.getFiles(
//      directoryToScan,
//      workspace,      
//      includeSubdirectories,
//      patternToUse,
//      run,
//      listener
//    );
//  }
  
  @Override
  public DescriptorImplementation getDescriptor() {
    return (DescriptorImplementation) super.getDescriptor();
  }
  
  public String getTask() {
    return "Protecode SC";
  }
  
  
  @Extension @Symbol("ProtecodeSC")
  public static final class DescriptorImplementation extends BuildStepDescriptor<Builder> implements ExtensionPoint {
    @Getter @Setter protected String protecodeScHost;
    @Getter @Setter protected boolean dontCheckCert;

    public DescriptorImplementation() {
      LOGGER.warning("BUILDING NEW DESCRIPTOR, loading from super");
      super.load();
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public boolean configure(StaplerRequest req, JSONObject formData)
      throws Descriptor.FormException {
      // To persist global configuration information,
      // set that to properties and call save().
      try {
        new URL(formData.getString("protecodeScHost"));
        this.protecodeScHost = formData.getString("protecodeScHost");
      } catch (MalformedURLException e) {

      }
      this.dontCheckCert = formData.getBoolean("dontCheckCert");

      save();
      return super.configure(req, formData);
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context) {
      // TODO Find a nice way to use this to fetch possible groups
      //  - this might be impossible in this scope
      StandardListBoxModel result = new StandardListBoxModel();
      result.withEmptySelection();
      result.withMatching(
        CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(
          StandardUsernamePasswordCredentials.class)),
        CredentialsProvider.lookupCredentials(
          StandardUsernamePasswordCredentials.class, context,
          ACL.SYSTEM,
          new HostnameRequirement(protecodeScHost)));
      return result;
    }

    public FormValidation doCheckProtecodeScHost(@QueryParameter String protecodeScHost)
      throws IOException, ServletException {
      try {
        URL protecodeHost = new URL(protecodeScHost);
        this.protecodeScHost = protecodeHost.toExternalForm();
        return FormValidation.ok();
      } catch (MalformedURLException e) {
        return FormValidation.error("Please provide a valid URL");
      }
    }

    public FormValidation doCheckPattern(@QueryParameter String pattern) {
      try {
        Pattern.compile(pattern);      
        return FormValidation.ok();
      } catch (Exception e) {
        return FormValidation.error("Please provide a valid Java style regexp pattern or leave "
          + "empty to include all files. Please see: https://docs.oracle.com/javase/8/docs/api/java/"
          + "util/regex/Pattern.html");
      }
    }

    public FormValidation doCheckProtecodeScGroup(@QueryParameter String protecodeScGroup) {
      try {
        Integer.parseInt(protecodeScGroup);    
        return FormValidation.ok();
      } catch (Exception e) {
        return FormValidation.error("Please provide a valid group. The group should a plain number,"
          + "not a URL or a name.");
      }
    }

    public FormValidation doCheckDirectoryToScan(@QueryParameter String directoryToScan) {
      // TODO: Make this work with empty string also... Currently börken
//      if (directoryToScan.matches(".*[^w$ -.y].*")) {
//        return FormValidation.ok();
//      } else {
//        return FormValidation.error("Cannot read given path, please double check it.");
//      }
      return FormValidation.ok();
    }

    public FormValidation doCheckTimeout(@QueryParameter String timeout) {
      try {
        Integer.parseInt(timeout);    
        return FormValidation.ok();
      } catch (Exception e) {
        return FormValidation.error("Please provide a valid timeout in minutes.");
      }
    }

    @Override
    public String getDisplayName() {
      return "Protecode SC";
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }
  }

  @DataBoundSetter
  public void setCredentialsId(String credentialsId) {
    this.credentialsId = credentialsId;
  }
  
  @DataBoundSetter
  public void setProtecodeScGroup(String protecodeScGroup) {
    this.protecodeScGroup = protecodeScGroup;
  }
  
  @DataBoundSetter
  public void setDirectoryToScan(String directoryToScan) {
    this.directoryToScan = directoryToScan;
  }
  
  @DataBoundSetter
  public void setIncludeSubdirectories(boolean includeSubdirectories) {
    this.includeSubdirectories = includeSubdirectories;
  }
  
  @DataBoundSetter
  public void setPattern(String pattern) {
    this.pattern = pattern;
  }
  
  @DataBoundSetter
  public void setConvertToSummary(boolean convertToSummary) {
    this.convertToSummary = convertToSummary;
  }
  
  @DataBoundSetter
  public void setFailIfVulns(boolean failIfVulns) {
    this.failIfVulns = failIfVulns;
  }
  
  @DataBoundSetter
  public void setScanTimeout(int scanTimeout) {
    this.scanTimeout = scanTimeout;
  }
    
  @CheckForNull
  public boolean getConvertToSummary() {
    return convertToSummary;
  }
  
  @CheckForNull
  public String getCredentialsId() {
    return credentialsId;
  }
  
  @CheckForNull
  public String getDirectoryToScan() {
    return directoryToScan;
  }
  
  @CheckForNull
  public boolean getIncludeSubdirectories() {
    return includeSubdirectories;
  }
  
  @CheckForNull
  public String getPattern() {    
    return pattern;
  }
  
  @CheckForNull
  public String getProtecodeScGroup() {
    return protecodeScGroup;
  }
  
  @CheckForNull
  public boolean getFailIfVulns() {
    return failIfVulns;
  }
  
  @CheckForNull
  public int getScanTimeout() {
    return scanTimeout;
  }
  
//  @Extension
//  public static boolean lastBuildSuccess() {
//    return true;
//  }
}