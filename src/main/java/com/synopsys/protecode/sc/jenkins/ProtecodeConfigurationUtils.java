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
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import hudson.security.ACL;
import jenkins.model.Jenkins;


public final class ProtecodeConfigurationUtils {
    private ProtecodeConfigurationUtils() {
        // don't instantiate me
    }
    
    public static StandardUsernamePasswordCredentials getCredentials(
        ProtecodeScPlugin.DescriptorImpl descriptor,
        String credentialsId
    ) {
        String host = descriptor.getProtecodeScHost();
        StandardUsernamePasswordCredentials creds = CredentialsMatchers
            .firstOrNull(
                CredentialsProvider.lookupCredentials(
                    StandardUsernamePasswordCredentials.class,
                    Jenkins.getInstance(), ACL.SYSTEM,
                    new HostnameRequirement(host)),
                CredentialsMatchers.withId(credentialsId));
        if (creds == null) {
            throw new RuntimeException("no such credentials for host");
        }
        return creds;
    }   
}
