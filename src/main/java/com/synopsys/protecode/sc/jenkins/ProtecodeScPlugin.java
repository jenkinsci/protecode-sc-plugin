package com.synopsys.protecode.sc.jenkins;


import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.ScanResultResponse;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.UploadResponse;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes.FileAndResult;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Item;
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
import javax.servlet.ServletException;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import okhttp3.MediaType;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class ProtecodeScPlugin extends Builder {
    @Getter @Setter private String credentialsId;
    @Getter @Setter private String protecodeScGroup;    
    @Getter @Setter private String artifactDir;
    @Getter @Setter private boolean convertToSummary = true;
    @Getter @Setter private boolean failIfVulns;
    @Getter @Setter private boolean leaveArtifacts;
    @Getter @Setter private int scanTimeout;  
    // don't access service directly, use service(). It checks w
    private ProtecodeScService service = null;
    
    // Below used in the scan process
    private List<FileAndResult> results = new ArrayList<>();    
    
    @DataBoundConstructor
    public ProtecodeScPlugin(
        String credentialsId, 
        String protecodeScGroup,        
        String artifactDir, 
        boolean convertToSummary,
        boolean failIfVulns,
        boolean leaveArtifacts, 
        int scanTimeout
    ) {
        System.out.println("-------- ProtecodeScPlugin");
        this.credentialsId = credentialsId;
        this.protecodeScGroup = protecodeScGroup;
        this.artifactDir = artifactDir;
        this.convertToSummary = convertToSummary;
        this.failIfVulns = failIfVulns;
        this.leaveArtifacts = leaveArtifacts;
        this.scanTimeout = scanTimeout > 10 ? scanTimeout : 10;
    }       
    
    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        if (getDescriptor().getProtecodeScHost() == null) {
            listener.error(
                    "Protecode SC host not defined. Configure it to global plugin properties");
            return false;
        }       
        // TODO check whether group is ok
        return true;
    }
    
    private ProtecodeScService service() {
        if (service == null) {
            try {
            service = ProtecodeScService.getInstance(
                credentialsId,
                new URL(getDescriptor().getProtecodeScHost())
            );
            } catch (MalformedURLException e) {
                // this url is already cleaned when getting it from the configuration page
            }
        }
        return service;
    }
    
    @Override    
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, 
        BuildListener listener) throws InterruptedException, IOException 
    {               
        PrintStream log = listener.getLogger();
        // use shortened word to distinguish from possibly null service
        ProtecodeScService serv = service();
        List<ReadableFile> filesToScan = Utils.getFiles(artifactDir, build, listener);
        
        for (ReadableFile file: filesToScan) {
            log.print("File: " + file.name());
            serv.scan(
                protecodeScGroup, 
                file.name(), 
                new StreamRequestBody
                (
                    MediaType.parse("application/octet-stream"), 
                    file.read()
                ), 
                (UploadResponse resp) -> {
                    addUploadResponse(file.name(), resp);
                }
            );   
        }
        
        // Then we wait and continue only when we have as many UploadResponses as we have 
        // filesToScan. Sad but true       
        log.print("Calling wait");
        waitForUploadResponses(filesToScan.size(), log);

        // start polling for reponses to scans
        
        return true;
    }
    
     /**
     * Called by the lamdas given to upload rest calls
     * @param response The responses fetched from Protecode SC
     */
    private void addUploadResponse(String name, UploadResponse response) {
        results.add(new FileAndResult(name, response));
    }
    
    /**
     * TODO clean up depth, this is awful
     * @param listener 
     */
    private void poll(BuildListener listener) {
        // use shortened word to distinguish from possibly null service
        ProtecodeScService serv = service();
        do {            
            results.forEach((fileAndResult) -> {
                if (!fileAndResult.ready()) {  // if this return true, we can ignore the fileAndResult
                    if (fileAndResult.uploadHTTPStatus() == 200) {                        
                        if ("R".equals(fileAndResult.getState())) {
                            fileAndResult.setResultBeingFetched(true);
                            serv.scanResult(
                                fileAndResult.getUploadResponse().getResults().getSha1sum(), 
                                (ScanResultResponse scanResult) -> {
                                    fileAndResult.setResultResponse(scanResult);
                                }
                            );
                        } else {
                            serv.poll(
                                fileAndResult.getUploadResponse().getResults().getId(), 
                                (UploadResponse uploadResponse) -> {
                                    fileAndResult.setUploadResponse(uploadResponse);                                
                                }
                            );
                        }
                    } else {
                        listener.error("Status code for file upload: '" + fileAndResult.getFilename() + 
                            "' was " + fileAndResult.uploadHTTPStatus());
                    }
                }
            });
        } while (allNotReady());
    }
    
    private boolean allNotReady() {
        return results.stream().anyMatch((fileAndResult) -> (!fileAndResult.ready()));
    }
    
    private synchronized void waitForUploadResponses(int fileCount, PrintStream log) {
        log.print("Starting wait");
        boolean waitForResponses = true;
        while (waitForResponses) {                   
            try {
                this.wait(1000);
                log.print("Tick");
                if (uploadResponses.size() == fileCount) {
                    waitForResponses = false;
                }
            } catch (InterruptedException ie) {
                log.print("Interrupted");
            }
        }
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        System.out.println("------------- getDescriptor");        
        return (DescriptorImpl) super.getDescriptor();
    }
    
    public String getTask() {
        return "Protecode SC";
    }
    
    // TODO: move to different file, this clutters
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> implements ExtensionPoint {        
        @Getter @Setter private String protecodeScHost;
        @Getter @Setter private boolean dontCheckCert;
        /* These are used to combine credentials with group to check is the combination valid */
        private String credentialsForCheck = null;
        private String groupForCheck = null;
                
        public DescriptorImpl() {           
            super.load();           
        }
        
        @Override
        @SuppressWarnings("ResultOfObjectAllocationIgnored")
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws Descriptor.FormException {
            System.out.println("------- configure");
            // To persist global configuration information,
            // set that to properties and call save().
            try {
                new URL(formData.getString("protecodeScHost"));
                protecodeScHost = formData.getString("protecodeScHost");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }           
            dontCheckCert = formData.getBoolean("dontCheckCert");

            save();
            return super.configure(req, formData);
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context) {
            // The host must have been set before this!
            // TODO find a nice way to tell the user to fill in the host first
            Utils.log("---------------------------------- doFillCreds");
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

        public FormValidation doCheckScanTimeout(@QueryParameter String value) 
            throws IOException, ServletException {
            Utils.log("validate timeout");
            try {
                Integer.parseInt(value);
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("Not a number");
            }
        }

        public FormValidation doCheckProtecodeScHost(@QueryParameter String protecodeScHost)            
            throws IOException, ServletException {
            System.out.println("checking host");
            try {
                URL protecodeHost = new URL(protecodeScHost);
                this.protecodeScHost = protecodeHost.toExternalForm();
                return FormValidation.ok("everything nice with url!");
            } catch (NumberFormatException e) {
                return FormValidation.error("The url provided was not formatted correctly");
            }
        }
        
        public FormValidation doCheckProtecodeScGroup(@QueryParameter String protecodeScGroup) {           
            Utils.log("group check");
            groupForCheck = protecodeScGroup;
            return FormValidation.ok();
        }

        public FormValidation doCheckCredentialsId(@QueryParameter String credentialsId) {           
            Utils.log("cred check");
            credentialsForCheck = credentialsId;
            return FormValidation.ok();
        }
        
        private boolean checkConnection(String group, String credentialsId) {
            return true;
        }
        
        @Override
        public String getDisplayName() {
            return "New Plugin!";
        }          

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            System.out.println("------- isApplicable");
            return true;
        }   
    }
}