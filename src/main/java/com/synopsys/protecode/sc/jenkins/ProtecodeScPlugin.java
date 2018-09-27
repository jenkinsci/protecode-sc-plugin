/** *****************************************************************************
 * Copyright (c) 2017 Synopsys, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Synopsys, Inc - initial implementation and documentation
 ****************************************************************************** */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.types.BuildVerdict;
import com.synopsys.protecode.sc.jenkins.types.FileResult;
import com.synopsys.protecode.sc.jenkins.utils.JenkinsConsoler;
import com.synopsys.protecode.sc.jenkins.utils.ReportBuilder;
import com.synopsys.protecode.sc.jenkins.utils.UtilitiesFile;
import com.synopsys.protecode.sc.jenkins.utils.UtilitiesGeneral;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;
import jenkins.tasks.SimpleBuildStep;

import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.synopsys.protecode.sc.jenkins.utils.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;

import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;

/**
 * TODO: There are much too many variables stored on the object level. Maybe we could perhaps store
 * them in a configuration object or much more preferably as temp variables being moved in the
 * methods. This would eliminate the danger of having a variable instantiated to an old value from
 * an older build.
 */
public class ProtecodeScPlugin extends Builder implements SimpleBuildStep {

  private String credentialsId;
  private String protecodeScGroup; // TODO: Group can be an integer
  private String directoryToScan;
  private String protecodeScanName;
  private String customHeader;
  private boolean includeSubdirectories;
  private String pattern; // Be carefull with this.
  /** Will cause the plugin to use a Jenkins service to fetch only artifacts from the specified directory */
  private boolean scanOnlyArtifacts;
  private boolean convertToSummary;
  private boolean failIfVulns;
  private int scanTimeout;

  // transients for old conf
  private transient String filesToScanDirectory;
  private transient String artifactDir;
  private transient boolean leaveArtifacts;

  // don't access service directly, use service(). It checks whether this exists
  private ProtecodeScService service = null;

  // used to know whether to make a new service
  private static URL storedHost = null;
  private static boolean storedDontCheckCertificate = true;

  // used for printing to the jenkins console
  private PrintStream log = null;
  private TaskListener listener = null;

  public static final String NO_ERROR = ""; // TODO: Use Optional
  private static final Logger LOGGER = Logger.getLogger(ProtecodeScPlugin.class.getName());
  private JenkinsConsoler console;

  @DataBoundConstructor
  public ProtecodeScPlugin(
    String credentialsId,
    String protecodeScGroup
  ) {
    this.credentialsId = credentialsId;
    this.protecodeScGroup = protecodeScGroup;
    this.includeSubdirectories = false;
    this.scanOnlyArtifacts = false;
    this.directoryToScan = "";
    this.customHeader = "";
    this.pattern = "";
    this.protecodeScanName = "";
    this.convertToSummary = false;
    this.failIfVulns = true;
    this.scanTimeout = 10;
  }

  /**
   * For backward compatibility. The XML persistence will build the instance in memory with out much
   * logic and since some values are empty, they will default to null. This method is called right
   * after the "resurrection" of the object and checks all non-trivial values.
   *
   * @return a ProtecodeScPlugin object with values which might be null.
   */
  public Object readResolve() {
    LOGGER.finer("readResolve: Initializing plugin object.");
    // Pattern
    if (pattern == null) {
      pattern = UtilitiesFile.ALL_FILES_REGEX_STRING;
    }

    // filesToScanDirectory -> directoryToScan
    if (filesToScanDirectory != null && directoryToScan == null) {
      this.directoryToScan = this.filesToScanDirectory;
    }
	
    // customHeader
    if (customHeader == null) {
      customHeader = "";
    }
	
    if (this.protecodeScanName == null) {
      this.protecodeScanName = "defaultbuildname";
    }
  
    return this;
  }

  private ProtecodeScService service() {
    // TODO: Add check that service is ok. Write http interceptor for okhttp

    // TODO: Is this needed? 
    getDescriptor().load();

    try {
      if (service == null        
        // We need to check whether we need a new instance of the backend.
        || !getDescriptor().getProtecodeScHost().equals(storedHost.toExternalForm())
        || getDescriptor().isDontCheckCert() != storedDontCheckCertificate
      ) {
        LOGGER.finer("Making new protecode http connection service");
        storedHost = new URL(getDescriptor().getProtecodeScHost());
        storedDontCheckCertificate = getDescriptor().isDontCheckCert();

        service = new ProtecodeScService(
          credentialsId,
          storedHost,
          !getDescriptor().isDontCheckCert()
        );
      }
    } catch (MalformedURLException e) {
      LOGGER.warning("No URL given for Protecode SC ");
      listener.error("Cannot read Protecode SC URL, please make sure it has been set in the Jenkins"
        + " configuration page.");
      // TODO: Add prebuild
    }
    return service;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
    throws InterruptedException, IOException {
    // TODO add try - catch (Interrupted) to call abort scan in protecode sc (remember to throw the
    // same exception upward)
    LOGGER.finer("Perform() with run object");
    this.listener = listener;
    doPerform(run);
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
    BuildListener listener) throws InterruptedException, IOException {
    // TODO add try - catch (Interrupted) to call abort scan in protecode sc (remember to throw the
    // same exception upward)
    LOGGER.finer("Perform() with build object");
    this.listener = listener;
    return doPerform(build);
  }
  
  public boolean doPerform(Run<?, ?> run)
    throws IOException, InterruptedException {
    
    FilePath workspace;
    try {
      workspace = run.getExecutor().getCurrentWorkspace();
    } catch (NullPointerException e) {
      listener.error("No executor workspace, exiting. Has the build been able to create a workspace?");
      run.setResult(Result.FAILURE);
      return false;
    }
    
    log = listener.getLogger();
    console = new JenkinsConsoler(listener);
  
    String cleanJob = null;
    if (protecodeScanName == null || "".equals(protecodeScanName)) {
      LOGGER.info("Didn't find job name, defaulting to build id");
      cleanJob = UtilitiesJenkins.cleanJobName(run.getExternalizableId()) != null 
        ? UtilitiesJenkins.cleanJobName(run.getExternalizableId()) : "jenkins_job";
    }
    
    console.start(failIfVulns, includeSubdirectories, protecodeScGroup);   
    BuildVerdict verdict = new BuildVerdict(failIfVulns);
          
    // use shortened word to distinguish from possibly null service
    ProtecodeScService serv = service();
    if (serv == null) {
      listener.error("Cannot connect to Protecode SC"); // TODO use consoler also
      run.setResult(Result.FAILURE);
      return false;
    }

    @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    String checkedDirectoryToScan = (null != getDirectoryToScan()) ? getDirectoryToScan() : "";
    
    Scanner scanner = new Scanner(
      verdict,
      protecodeScGroup,
      serv,
      run,
      scanTimeout,
      workspace,
      listener,
      directoryToScan,
      scanOnlyArtifacts,
      includeSubdirectories,
      pattern,
      protecodeScanName,
      customHeader,
      console
    );
    
    // Get/scan the files
    List<FileResult> results = null;
    try {
      results = scanner.doPerform();
    } catch (IOException ioe) {
      listener.error("Could not send files to Protecode-SC: " + ioe);
      //return false;
    } catch (InterruptedException ie) {
      log.println("Interrupted, stopping build");
      run.setResult(Result.ABORTED);
      return false;
    }
    
    if (verdict.getFilesFound() == 0) {
      console.log("Could not find any files to scan. Skipping build step with 'no error' status");
      return true;
    }
    
    // make results
    ReportBuilder.report(results, listener, UtilitiesFile.reportsDirectory(run), run);

    // summarise
    if (convertToSummary) {
      log.println("Writing summary for summary plugin to protecodesc.xml");
      ReportBuilder.makeSummary(run, listener);
    }

    //evaluate, if verdict is false, there are vulns
    ProtecodeEvaluator.evaluate(results, verdict);    
    boolean buildStatus = false;
    
    if (failIfVulns) {
      if (!verdict.verdict()) {
        console.printReportString(results);
        listener.fatalError("Vulnerabilities found. Failing build.");
        run.setResult(Result.FAILURE);
      }
      buildStatus = verdict.verdict();
    } else {
      if (!verdict.verdict()) {
        log.println("Vulnerabilities found! Not failing build due to configuration.");
      } else {
        log.println("NO vulnerabilities found.");
      }
      buildStatus = true;
      run.setResult(Result.SUCCESS);
    }

    log.println("/---------- Protecode SC plugin end -----------/");
    // TODO: Use perhaps unstable also
    return buildStatus;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public String getTask() {
    return "Protecode SC";
  }

  @Extension
  @Symbol("protecodesc")
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> implements ExtensionPoint {
    /** Read from jelly */
    public static final int defaultTimeout = 10;
    /** Read from jelly */
    public static final boolean defaultFailIfVulns = true;

    @Getter @Setter
    protected String protecodeScHost;
    
    @Getter @Setter
    protected boolean dontCheckCert;

    public DescriptorImpl() {
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
      // https://groups.google.com/forum/?hl=en#!searchin/jenkinsci-dev/store$20configuration|sort:date/jenkinsci-dev/-DosteCUiu8/18-HvlAsAAAJ
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

    @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE") // It's supposed to blow up... 
    public FormValidation doCheckCustomHeader(@QueryParameter String customHeader) {
      try {
        if (customHeader == null || "".equals(customHeader)) {
          return FormValidation.ok();
        }
        ObjectReader reader = new ObjectMapper().readerFor(Map.class);
        final Map<String, String> map = reader.readValue(customHeader);
        return FormValidation.ok();
      } catch (IOException | NullPointerException e) {
        return FormValidation.error("Please provide a key-value list in JSON format.");
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
  
  @DataBoundSetter
  public void setScanOnlyArtifacts(boolean onlyArtifacts) {
    this.scanOnlyArtifacts = onlyArtifacts;
  }

  @DataBoundSetter
  public void setCustomHeader(String customHeader) {
    this.customHeader = customHeader;
  }
  
  @DataBoundSetter
  public void setProtecodeScanName(String protecodeScanName) {
    this.protecodeScanName = protecodeScanName;
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
  
  @CheckForNull
  public boolean getScanOnlyArtifacts() {
    return scanOnlyArtifacts;
  }
  
  @CheckForNull
  public String getCustomHeader() {
    return customHeader;
  }
  
  @CheckForNull
  public String getProtecodeScanName() {
    return this.protecodeScanName;
  }
}
