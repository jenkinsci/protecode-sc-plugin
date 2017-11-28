package com.synopsys.protecode.sc.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.tasks.Builder;
import java.io.IOException;
import java.net.URL;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.stapler.DataBoundConstructor;

public class ProtecodeScPlugin extends Builder {
            
    @Getter @Setter private String protecodeScGroup; // gotten from getGroup, and the pull down in UI
    @Getter @Setter private String credentialsId;
    @Getter @Setter private String artifactDir;
    @Getter @Setter private boolean convertToSummary = true;
    @Getter @Setter private boolean failIfVulns;
    @Getter @Setter private boolean leaveArtifacts;
    @Getter @Setter private int scanTimeout;

    @DataBoundConstructor
    public ProtecodeScPlugin(
        String credentialsId, 
        String protecodeScGroup,
        boolean failIfVulns, 
        String artifactDir, 
        boolean convertToSummary,
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().print("-------- perform, group: " + protecodeScGroup);
        return true;
    }
    
    @Override
    public Descriptor getDescriptor() {
        System.out.println("getDescriptor");
        return new DescriptorImpl();
    }
    
    @Extension
    public static final class DescriptorImpl extends ProtecodeConfigurationDescriptor {
        public DescriptorImpl() {
            super();
        }
    }
}