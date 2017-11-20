package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.types.Group;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
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
        System.out.println("AAAafddAAAafddfAAAafddfAAAafddfAAAafddfAAAafddfAAAafddfAAAafddfAAAafddff");
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
//        System.out.println("BOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOBBOB");
        listener.getLogger().print("BOBOOOOBBBBOBOBOBOBOBOBOBOBOBOOBOBOBOBOBOBOBOBOOB");
        return true;
    }
    
    @Override
    public Descriptor getDescriptor() {
        return new DescriptorImpl();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private Group [] groups;
        
        // creds?
        
        public DescriptorImpl() { 
            super();
            System.out.println("DescriptorImplDescriptorImplDescriptorImplDescriptorImplDescriptorImpl");           
            super.load();
        }

        @Override
        public String getDisplayName() {
            return "New Plugin!";
        }

        @Override
        public synchronized void load() {
            super.load(); 
        }           

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
    
    public String getBob() {
        return "Bob";
    }
}