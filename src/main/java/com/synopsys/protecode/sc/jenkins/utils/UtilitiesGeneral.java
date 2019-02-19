/** *****************************************************************************
 * Copyright (c) 2017 Synopsys, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Synopsys, Inc - initial implementation and documentation
 ****************************************************************************** */
package com.synopsys.protecode.sc.jenkins.utils;

import com.synopsys.protecode.sc.jenkins.types.ConnectionStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import okhttp3.Headers;

public final class UtilitiesGeneral {

  private static final List<String> PUBLIC_HOSTS = new ArrayList<>();

  static {
    // Add possible names for cloud instance. This will limit the possiblity of choosing
    // "do not zip files". This limitation is in place to protect the cloud if the user
    // would happen to recursively send large amounts of files.
    PUBLIC_HOSTS.add("protecode-sc.com");
  }

  /**
   * Checks connection status for errors.
   *
   * @param connectionStatus the status object to check
   * @return true for connection ok, otherwise false
   */
  public static boolean connectionOk(ConnectionStatus connectionStatus) {
    if (connectionStatus.getError().isPresent()) {
      return false;
    }
    return !Integer.toString(connectionStatus.code()).startsWith("4");
  }

  /**
   * Checks the headers to determine whether BDBA is up and running
   *
   * @param headers headers from response
   * @param code the http response code
   * @return true if connection seems to be ok
   */
  public static boolean checkResponse(Headers headers, int code) {
    // TODO: Check if we should check the headers also
    return !Integer.toString(code).startsWith("4");
  }

  /*
  * See that is the host like the fingerprints in the public hosts list
  *
  * This method exists to see if we should act accordingly when using some host.
  */
  public static boolean isPublicHost(String host) {
    return PUBLIC_HOSTS.stream().anyMatch((public_host) -> {
      return host.contains(public_host);
    });
  }

  /**
   * Replaces spaces with underscore in the given line. This is used to format the query parameter *
   *
   * in the call to BDBA. The problem is that BDBA has a limited acceptable chars * group.
   *
   * TODO: Use perhaps a regexp maybe?
   * TODO: There is a slight possiblity that the user will give a file with underscores and a file
   * with spaces which are otherwise identical. This will then not work.
   *
   * @param line The string to format
   * @return A string with spaces replaced with underscore
   */
  public static String replaceSpaceWithUnderscore(String line) {
    // TODO, use something which is certainly not used in other files. Underscore isn't good.
    // Currently underscore is accepted in BDBA so it's in use.
    return line.replace(" ", "_");
  }

  /**
   * Method for getting a nicely formated timestamp.
   * TODO: This might already be implemented somewhere.
   * @return A formated time as string
   */
  public static String timestamp() {
    SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss");//dd/MM/yyyy
    Date now = new Date();
    String strDate = sdfDate.format(now);
    return strDate;
  }

  /**
   * Does a very simple check that is the string provided a valid URL.
   *
   * @param possibleUrl the string to verify a url
   * @return true if string provided is a valid URL
   */
  @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE")
  public static boolean isUrl(String possibleUrl) {
    try {
      final URL url = new URL(possibleUrl);
      return true;
    } catch(MalformedURLException e) {
      return false;
    }
  }
}
