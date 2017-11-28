/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import com.synopsys.protecode.sc.jenkins.interfaces.Listeners;
import com.synopsys.protecode.sc.jenkins.types.*;
import hudson.model.AbstractProject;
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
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class ProtecodeConfigurationDescriptor extends BuildStepDescriptor<Builder> {
    private InternalTypes.Group [] groups;
    private InternalTypes.Group chosenGroup;
    private URL protecodeScHost;
    private boolean dontCheckCert;
    private Credentials credentialsId;

    protected ProtecodeConfigurationDescriptor() { 
        super();
        System.out.println("-------- ProtecodeConfigurationDescriptor");           
        super.load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
            throws Descriptor.FormException {
        System.out.println("------- configure");
        // To persist global configuration information,
        // set that to properties and call save().
        try {
            protecodeScHost = new URL(formData.getString("protecodeScHost"));
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
        StandardListBoxModel result = new StandardListBoxModel();
        result.withEmptySelection();
        result.withMatching(
            CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(
                StandardUsernamePasswordCredentials.class)),
            CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class, context,
                ACL.SYSTEM,
                new HostnameRequirement(protecodeScHost.toExternalForm())));
        return result;
    }

    
    public ListBoxModel doFillProtecodeScGroupItems(@QueryParameter String protecodeScGroup) {
        System.out.println("start");
        final AtomicReference<HttpTypes.Groups> notifier = new AtomicReference();
        System.out.println("AtomicReference");
        ProtecodeScService.getInstance(
            
        ).groups((HttpTypes.Groups tempGroups) -> {
            synchronized (notifier) {
                System.out.println("synchronized (notifier) {");
                notifier.set(tempGroups);
                notifier.notify();
            }
        });
        synchronized (notifier) {
            System.out.println("synchronized (notifier) {");
            while (notifier.get() == null)          
            try {
                System.out.println("2222");
                notifier.wait();
            } catch (InterruptedException ex) {
                // Carry on... We're supposed to be here. Nothing to see.
            }
        }
        System.out.println("333333");
        HttpTypes.Groups groups = notifier.get();
        ListBoxModel listBoxModel = new ListBoxModel();
        for (HttpTypes.Group group : groups.getGroups()) {
            System.out.println("444444");
          listBoxModel.add(group.getName());                
        }
        System.out.println("55555");
        return listBoxModel;
    }

    public FormValidation doCheckScanTimeout(@QueryParameter String value) 
        throws IOException, ServletException {
        try {
            Integer.parseInt(value);
            return FormValidation.ok();
        } catch (NumberFormatException e) {
            return FormValidation.error("Not a number");
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

    public String getProtecodeScHost() {
        System.out.println("------- getProtecodeScHost");
        return protecodeScHost.toExternalForm();
    }

    public boolean isDontCheckCert() {
        System.out.println("------- isDontCheckCert");
        return dontCheckCert;
    }       
}