package com.synopsys.protecode.sc.jenkins;

import hudson.tasks.Builder;
import lombok.Data;
import org.kohsuke.stapler.DataBoundConstructor;


/**
 *
 * @author pajunen
 */
public @Data class ProtecodeScPlugin extends Builder {
    
    private final String task;
    private String protecodeScGroup;
    private String credentialsId;
    private String artifactDir;
    private boolean convertToSummary = true;
    private boolean failIfVulns;
    private boolean leaveArtifacts;
    private int scanTimeout;
    
    @DataBoundConstructor
    public ProtecodeScPlugin(String task) {
        // Get conf here
        System.out.println("BOBS UR UNKKEL");
        this.task = task;
    }    
}
