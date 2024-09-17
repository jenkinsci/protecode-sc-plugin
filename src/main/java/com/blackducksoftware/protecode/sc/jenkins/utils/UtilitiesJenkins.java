/*******************************************************************************
 * Copyright (c) 2017 Black Duck Software, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Black Duck Software, Inc - initial implementation and documentation
 *******************************************************************************/
package com.blackducksoftware.protecode.sc.jenkins.utils;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import hudson.security.ACL;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 * Utility for Jenkins related functionality.
 */
public final class UtilitiesJenkins {

  private static final Logger LOGGER = Logger.getLogger(UtilitiesJenkins.class.getName());

  private UtilitiesJenkins() {
    // don't instantiate me
  }

  /**
   * Returns The first suitable credential for the id.
   * @param url Host to associate the credentials with
   * @param credentialsId The id of the credentials
   * @return Standard credential types
   */
  public static StandardUsernamePasswordCredentials getCredentials(
    URL url,
    String credentialsId
  ) {
    StandardUsernamePasswordCredentials creds = CredentialsMatchers
      .firstOrNull(
        CredentialsProvider.lookupCredentials(
          StandardUsernamePasswordCredentials.class,
          Jenkins.getInstance(), ACL.SYSTEM,
          new HostnameRequirement(url.toExternalForm())),
        CredentialsMatchers.withId(credentialsId));
    LOGGER.log(Level.FINE, "Creds: {0}", creds);
    return creds;
  }

  /**
   * Returns only the first part of the job name. Currently the job name is composed of the job name
   * and the
   * number of the build, e.g. "somejob#7". BDBA doesn't accept "#" and the number   * should not be added
   *
   * @param jobName the jenkins build name to be cleaned for BDBA use
   * @return the first token before #, this is the "normal" job name.
   */
  public static String cleanJobName(String jobName) {
    return new StringTokenizer(jobName, "#").nextToken();
  }
}
