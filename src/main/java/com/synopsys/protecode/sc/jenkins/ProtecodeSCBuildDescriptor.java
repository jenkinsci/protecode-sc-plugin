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
import hudson.ExtensionPoint;
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
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * 
 */

public final class ProtecodeSCBuildDescriptor extends BuildStepDescriptor<Builder> implements ExtensionPoint {
  @Getter @Setter private String protecodeScHost;
    @Getter @Setter private boolean dontCheckCert;
    
    public ProtecodeSCBuildDescriptor() {
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
          + "empty to include all files.");
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
