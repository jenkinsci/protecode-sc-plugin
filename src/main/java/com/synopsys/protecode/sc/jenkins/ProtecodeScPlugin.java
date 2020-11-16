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

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.synopsys.protecode.sc.jenkins.Scanner;
import com.synopsys.protecode.sc.jenkins.exceptions.ApiException;
import com.synopsys.protecode.sc.jenkins.exceptions.ScanException;
import com.synopsys.protecode.sc.jenkins.types.BuildVerdict;
import com.synopsys.protecode.sc.jenkins.types.FileResult;
import com.synopsys.protecode.sc.jenkins.utils.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.*;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;

/**
 * TODO: There are much too many variables stored on the object level. Maybe we could perhaps store them in a
 * configuration object or much more preferably as temp variables being moved in the methods.
 */
public class ProtecodeScPlugin extends Builder implements SimpleBuildStep {

  private String credentialsId;
  private String protecodeScGroup; // TODO: Group can be an integer
  private String directoryToScan;
  private String protecodeScanName;
  private String customHeader;
  private boolean includeSubdirectories;
  private String pattern; // Be carefull with this.
  /**
   * Will cause the plugin to use a Jenkins service to fetch only artifacts from the specified directory
   */
  private boolean scanOnlyArtifacts;
  private boolean convertToSummary;
  private boolean failIfVulns;
  private boolean endAfterSendingFiles;
  private int scanTimeout;
  private boolean dontZipFiles;

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
  //private PrintStream log = null;
  private TaskListener buildListener = null;

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
    this.endAfterSendingFiles = false;
    this.scanTimeout = 10;
    this.dontZipFiles = false;
  }

  /**
   * For backward compatibility. The XML persistence will build the instance in memory with out much logic and
   * since some values are empty, they will default to null. This method is called right after the
   * "resurrection" of the object and checks all non-trivial values.
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

  private ProtecodeScService service(Run<?, ?> run) {
    // TODO: Add check that service is ok. Write http interceptor for okhttp

    // TODO: Is this needed?
    getDescriptor().load();

    try {
      if (service == null
        // We need to check whether we need a new instance of the backend.
        || !getDescriptor().getProtecodeScHost().equals(storedHost.toExternalForm())
        || getDescriptor().isDontCheckCert() != storedDontCheckCertificate) {
        LOGGER.finer("Making new " + Configuration.TOOL_NAME + " http connection service");
        storedHost = new URL(getDescriptor().getProtecodeScHost());
        storedDontCheckCertificate = getDescriptor().isDontCheckCert();

        // TODO: Make credentials provider since we don't want to provide the run context to parts which
        //   shouldn't be jenkins libs linked.
        service = new ProtecodeScService(
          credentialsId,
          storedHost,
          run,
          !getDescriptor().isDontCheckCert()
        );
        // TODO: Add username password check
        // Call some API which should always work and see if it doesn't return an error
      }
    } catch (MalformedURLException e) {
      LOGGER.warning("No URL given for " + Configuration.TOOL_NAME);
      buildListener.error("Cannot read " + Configuration.TOOL_NAME + " URL, please make sure it has been set in the Jenkins"
        + " configuration page.");
      // TODO: Add prebuild
    }
    return service;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
    throws InterruptedException, IOException {
    // TODO add try - catch (Interrupted) to call abort scan in BDBA (remember to throw the
    // same exception upward)
    LOGGER.finer("Perform() with run object");
    this.buildListener = listener;
    doPerform(run, workspace);
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
    BuildListener listener) throws InterruptedException, IOException {
    // TODO add try - catch (Interrupted) to call abort scan in BDBA (remember to throw the
    // same exception upward)
    LOGGER.finer("Perform() with build object");
    this.buildListener = listener;
    return doPerform(build, build.getWorkspace());
  }

  public boolean doPerform(Run<?, ?> run, FilePath workspace)
    throws IOException, InterruptedException {

    // TODO: Check credentials exists!
    if (workspace == null) {
      String message = "No executor workspace, exiting. Has the build been able to create a workspace?";
      buildListener.error(message);
      if (failIfVulns) {
        throw new ScanException(message);
      }

      return false;
    }

    //log = buildListener.getLogger();
    console = JenkinsConsoler.getInstance();
    console.setStream(buildListener.getLogger());

    String cleanJob = null;
    if (protecodeScanName == null || "".equals(protecodeScanName)) {
      LOGGER.info("Didn't find job name, defaulting to build id");
      cleanJob = UtilitiesJenkins.cleanJobName(run.getExternalizableId());
    } else {
      cleanJob = protecodeScanName;
    }

    console.start(failIfVulns, includeSubdirectories, protecodeScGroup);
    BuildVerdict verdict = new BuildVerdict(failIfVulns);

    // use shortened word to distinguish from possibly null service
    ProtecodeScService serv = service(run);
    if (serv == null) {
      String message = "Cannot connect to " + Configuration.TOOL_NAME;
      buildListener.error(message); // TODO use consoler also
      if (failIfVulns) {
        throw new ApiException(message);
      }

      return false;
    }

    // If we're using a Synopsys hosted protecode instance, we will use zipping in cases where there are more
    // than the set limit of files.
    boolean forceDontZip = this.dontZipFiles
      && !UtilitiesGeneral.isPublicHost(getDescriptor().getProtecodeScHost());
    if (this.dontZipFiles) {
      console.log("'Dont zip' is chosen, but since this build is done against a Synopsys hosted "
        + Configuration.TOOL_NAME + " instance, this option is ignored.");
    }

    // TODO: Make Scanner not linked to Jenkins.
    Scanner scanner = new Scanner(
      verdict,
      protecodeScGroup,
      serv,
      run,
      scanTimeout,
      workspace,
      buildListener,
      getDirectoryToScan(),
      scanOnlyArtifacts,
      includeSubdirectories,
      endAfterSendingFiles,
      pattern,
      cleanJob,
      customHeader,
      forceDontZip,
      failIfVulns
    );

    // Get/scan the files
    List<FileResult> results = new ArrayList<>();
    try {
      // There needs to be a possiblity to just end the phase after the files are transfered.
      Optional<List<FileResult>> resultOp = scanner.doPerform();
      if (verdict.getFilesFound() == 0) {
        LOGGER.info("No files found, ending " + Configuration.TOOL_NAME + " phase.");
        console.log("No files found, ending " + Configuration.TOOL_NAME + " phase.");
        return true;
      }
      if (endAfterSendingFiles) {
        LOGGER.info("Files sent, ending " + Configuration.TOOL_NAME + " phase due to configuration.");
        console.log("Files sent, ending phase.");
        return true;
      }
      results = resultOp.get();
    } catch (IOException ioe) {
      buildListener.error("Could not send files to " + Configuration.TOOL_NAME + ": " + ioe);
      verdict.setError("Could not send files to " + Configuration.TOOL_NAME);
      if(failIfVulns) {
        throw new ApiException("Could not send files to " + Configuration.TOOL_NAME);
      }
      if (results.isEmpty()) {
        return false;
      } // otherwise carry on, might get something
    } catch (InterruptedException ie) {
      String message = "Interrupted, stopping build";
      buildListener.error(message);
      console.log(message);
      if(failIfVulns) {
        throw new ScanException(message);
      }
      return false;
    }

    // make results
    ReportBuilder.report(results, buildListener, UtilitiesFile.reportsDirectory(run), run);

    // summarise
    if (convertToSummary) {
      console.log("Writing summary for summary plugin to protecodesc.xml");
      ReportBuilder.makeSummary(run, buildListener);
    }

    //evaluate, if verdict is false, there are vulns
    ProtecodeEvaluator.evaluate(results, verdict);
    boolean buildStatus = verdict.verdict();

    // TODO: W-E-T
    // TODO: This is awful. It will tell the user that some files have vulns when any error happened
    // even though that's not true. The list of vulnerable files will just be empty
    if (failIfVulns) {
      if (!verdict.verdict()) {
        console.printReportString(results);
        buildListener.fatalError(verdict.verdictStr());
        throw new ScanException(verdict.verdictStr());
      } else {
        console.log("NO vulnerabilities found.");
      }
    } else {
      if (!verdict.verdict()) {
        console.printReportString(results);
        console.log("Vulnerabilities/errors found! Not failing build due to configuration.");
      } else {
        console.log("NO vulnerabilities found.");
      }
      buildStatus = true;
    }

    console.log(Configuration.TOOL_NAME + " plugin end");
    // TODO: Use perhaps unstable also
    return buildStatus;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public String getTask() {
    return Configuration.TOOL_NAME;
  }

  @Extension
  @Symbol("protecodesc")
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> implements ExtensionPoint {

    /**
     * Read from jelly
     */
    public static final int defaultTimeout = 60;
    /**
     * Read from jelly
     */
    public static final boolean defaultFailIfVulns = true;
    public static final boolean defaultEndAfterSendingFiles = false;
    public static final boolean defaultDontZipFiles = false;

    @Getter
    @Setter
    protected String protecodeScHost;

    @Getter
    @Setter
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
      // TODO: use non-deprectated
      result.withEmptySelection();
      result.withMatching(
        CredentialsMatchers.anyOf(
          CredentialsMatchers.instanceOf(
            // TODO: Perhaps too wide
            StandardCredentials.class
          )
        ),
        CredentialsProvider.lookupCredentials(
          // TODO: Perhaps too wide
          StandardCredentials.class, context,
          ACL.SYSTEM,
          new HostnameRequirement(protecodeScHost)
        )
      );
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
      } catch (NumberFormatException e) {
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
      } catch (NumberFormatException e) {
        return FormValidation.error("Please provide a valid timeout in minutes.");
      }
    }

    @Override
    public String getDisplayName() {
      return Configuration.TOOL_NAME;
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

  @DataBoundSetter
  public void setEndAfterSendingFiles(boolean endAfterSendingFiles) {
    this.endAfterSendingFiles = endAfterSendingFiles;
  }

  @DataBoundSetter
  public void setDontZipFiles(boolean dontZipFiles) {
    this.dontZipFiles = dontZipFiles;
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
    if (directoryToScan != null) {
      return directoryToScan;
    } else {
      return ".";
    }
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

  @CheckForNull
  public boolean getEndAfterSendingFiles() {
    return this.endAfterSendingFiles;
  }

  @CheckForNull
  public boolean getDontZipFiles() {
    return this.dontZipFiles;
  }
}
