/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import hudson.security.ACL;
import jenkins.model.Jenkins;

/**
 *
 * @author pajunen
 */
public final class ProtecodeConfigurationUtils {
    private ProtecodeConfigurationUtils() {
        // don't instantiate me
    }
    
    public static StandardUsernamePasswordCredentials getCredentials(
        ProtecodeScPlugin.DescriptorImpl descriptor,
        String credentialsId
    ) {
        String host = descriptor.getProtecodeScHost().toExternalForm();
        StandardUsernamePasswordCredentials creds = CredentialsMatchers
            .firstOrNull(
                CredentialsProvider.lookupCredentials(
                    StandardUsernamePasswordCredentials.class,
                    Jenkins.getInstance(), ACL.SYSTEM,
                    new HostnameRequirement(host)),
                CredentialsMatchers.withId(credentialsId));
        if (creds == null) {
            System.out.println("No Protecode SC credentials found");
            throw new RuntimeException("no such credentials for host");
        }
        return creds;
    }
}
