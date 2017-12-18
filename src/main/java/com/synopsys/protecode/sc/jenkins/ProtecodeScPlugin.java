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
import com.synopsys.protecode.sc.jenkins.ProtecodeScService;
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
    @Getter @Setter private String filesToScanDirectory;
    @Getter @Setter private boolean convertToSummary = true;
    @Getter @Setter private boolean failIfVulns;
    @Getter @Setter private boolean leaveArtifacts;
    @Getter @Setter private int scanTimeout;  
    // don't access service directly, use service(). It checks whether this exists
    private ProtecodeScService service = null;
    
    // Below used in the scan process
    private final List<FileAndResult> results = new ArrayList<>(); 
    private long stopAt = 0;
    
    public static final String REPORT_DIRECTORY = "reports";
    
    @DataBoundConstructor
    public ProtecodeScPlugin(
        String credentialsId, 
        String protecodeScGroup,        
        String filesToScanDirectory, 
        boolean convertToSummary,
        boolean failIfVulns,
        boolean leaveArtifacts, 
        int scanTimeout
    ) {
        System.out.println("-------- ProtecodeScPlugin");
        this.credentialsId = credentialsId;
        this.protecodeScGroup = protecodeScGroup;
        this.filesToScanDirectory = filesToScanDirectory;
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
        // TODO: check whether group is ok
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
    
    private boolean isTimeout() {
        return System.currentTimeMillis() > stopAt;
    }
    
    private void startPollTimer() {
        // stopAt is set to be the moment we don't try to poll anymore
        stopAt = System.currentTimeMillis() + 1000L * 60 * scanTimeout;
    }
    
    @Override    
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, 
        BuildListener listener) throws InterruptedException, IOException 
    {               
        PrintStream log = listener.getLogger();
        // use shortened word to distinguish from possibly null service
        ProtecodeScService serv = service();
        List<ReadableFile> filesToScan = Utils.getFiles(filesToScanDirectory, build, listener);
        
        if (filesToScan.isEmpty()) {
            // no files to scan, no failure
            return true;
        } else {
            log.println("Directory for files to scan was not empty, proceding");            
        }
        
        log.println("Sending files");        
        
        for (ReadableFile file: filesToScan) {
            log.println("Sending file: " + file.name());            
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
        log.println("Calling wait");
        waitForUploadResponses(filesToScan.size(), log);        
        log.println("Wait over");
        
        // start polling for reponses to scans
        poll(listener);                
        
        //evaluate
        boolean verdict = ProtecodeEvaluator.evaluate(results, build, listener);
        
        // make results
        ReportBuilder.report(results, build, listener, REPORT_DIRECTORY);
            
        // summarise
        if(convertToSummary) {
            ReportBuilder.makeSummary(results, build, listener, REPORT_DIRECTORY);
        }
        
        return verdict;
    }
    
     /**
     * Called by the lamdas given to upload rest calls
     * @param response The responses fetched from Protecode SC
     */
    private void addUploadResponse(String name, UploadResponse response) {
        results.add(new FileAndResult(name, response));
    }
    
    /**
     * TODO clean up depth, move logic to other methods.
     * @param listener 
     */
    private void poll(BuildListener listener) {
        startPollTimer();
        PrintStream log = listener.getLogger();
        // use shortened word to distinguish from possibly null service
        ProtecodeScService serv = service();
        do {
            if (isTimeout()) {
                break;
            }
            results.forEach((fileAndResult) -> {
                log.println("Starting result polling and fetching");
                if (!fileAndResult.ready()) {  // if this return true, we can ignore the fileAndResult                    
                    log.println("no result received yet for " + fileAndResult.getFilename());
                    if (fileAndResult.uploadHTTPStatus() == 200) {     
                        log.println("HTTP Status for " + fileAndResult.getFilename() + " is 200, proceding");
                        if ("R".equals(fileAndResult.getState()) && !fileAndResult.isResultBeingFetched()) {
                            log.println("status not R yet for " + fileAndResult.getFilename());
                            fileAndResult.setResultBeingFetched(true);
                            serv.scanResult(
                                fileAndResult.getUploadResponse().getResults().getSha1sum(), 
                                (ScanResultResponse scanResult) -> {                                    
                                    log.println("setting result for file: " + fileAndResult.getFilename());
                                    fileAndResult.setResultResponse(scanResult);
                                }
                            );
                        } else {
                            log.println("status is R for " + fileAndResult.getFilename() + ", fetching scan result");
                            serv.poll(
                                fileAndResult.getUploadResponse().getResults().getId(), 
                                (UploadResponse uploadResponse) -> {
                                    log.println("server responded for query of scan result for " + fileAndResult.getFilename());
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
            // TODO, don't sleep when all is done
            try {
                log.println("Sleeping for a moment");
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                log.println("Sleep was interrupted");
            }            
        } while (allNotReady());
        log.println("All results in, returning to perform()");
    }
    
    private boolean allNotReady() {
        return results.stream().anyMatch((fileAndResult) -> (!fileAndResult.ready()));
    }
    
    /**
     * Waits until all upload results are in. Returns only then.
     * @param fileCount How many files were uploaded
     * @param log for printing to Jenkins build console.
     */
    private void waitForUploadResponses(int fileCount, PrintStream log) {
        log.println("Starting wait");
        boolean waitForResponses = true;
        while (waitForResponses) {                   
            try {                
                Thread.sleep(10 * 1000);
                // TODO: remove print after testing
                log.println("Tick - remove this");
                if (results.size() == fileCount) {
                    waitForResponses = false;
                }
            } catch (InterruptedException ie) {
                log.println("Interrupted");
            }
        }
    }        
    
    @Override
    public DescriptorImpl getDescriptor() {      
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
                protecodeScHost = formData.getString("protecodeScHost");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }           
            dontCheckCert = formData.getBoolean("dontCheckCert");

            save();
            return super.configure(req, formData);
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context) {            
            // TODO Find a nice way to use this to fetch possible groups
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
                return FormValidation.ok("everything nice with url!");
            } catch (NumberFormatException e) {
                return FormValidation.error("The url provided was not formatted correctly");
            }
        }
        
        @Override
        public String getDisplayName() {
            // TODO: give a nicer name 
            return "New Plugin!";
        }          

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }   
    }
}