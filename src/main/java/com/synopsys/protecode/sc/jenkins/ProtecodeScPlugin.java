package com.synopsys.protecode.sc.jenkins;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes;
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
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletException;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().print("-------- perform, group: " + protecodeScGroup);
        return true;
    }
    
    @Override
    public Descriptor getDescriptor() {
        System.out.println("------------- getDescriptor");        
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> implements ExtensionPoint {        
        private String protecodeScHost;
        private boolean dontCheckCert;
                
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

        public ListBoxModel doFillProtecodeScGroupItems(@QueryParameter String protecodeScGroup) {
            return new ListBoxModel(new ListBoxModel.Option("1.13", "1.13", protecodeScGroup.matches("1.13") ),
                    new ListBoxModel.Option("1.14", "1.14", protecodeScGroup.matches("1.14") ),
                    new ListBoxModel.Option("1.15", "1.15", protecodeScGroup.matches("1.15") ));
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
            try {
                URL protecodeHost = new URL(protecodeScHost);
                this.protecodeScHost = protecodeHost.toExternalForm();
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("The url provided was not formatted correctly");
            }
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