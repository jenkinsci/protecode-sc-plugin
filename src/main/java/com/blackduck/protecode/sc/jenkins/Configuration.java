/** *****************************************************************************
 * Copyright (c) 2018 Black Duck Software, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Black Duck Software, Inc - initial implementation and documentation
 ****************************************************************************** */
package com.blackduck.protecode.sc.jenkins;

/**
 * A convenient location to store variables which can easily be found and edited
 */
public class Configuration {

  /**
   * The maximum simultaneous requests to BDBA backend. Current maximum is 4, more requests will throw 503,
   * "service unavailable" sometimes
   */
  public static final int MAX_REQUESTS_TO_PROTECODE = 4;

  /**
   * This is the limit above which files will be zipped always when using cloud and when using other target
   * with the zipping enabled.
   */
  public static final int MAXIMUM_UNZIPPED_FILE_AMOUNT = 50;

  /**
   * Directory to store json reports for scans
   */
  public static final String REPORT_DIRECTORY = "reports";

  /**
   * Connection read/write timeout.
   */
  public static final int TIMEOUT_SECONDS = 5000;

  /**
   * Client name to be used in http headers.
   */
  // TODO: Get name with version also. This could be done by using resources.
  public static final String CLIENT_NAME = "Protecode Jenkins Plugin";

  /**
   * Convenience for making sure the tool name is correct.
   */
  public static final String TOOL_NAME = "Black Duck Binary Analysis";
}
