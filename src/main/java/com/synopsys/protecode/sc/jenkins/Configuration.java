/*******************************************************************************
* Copyright (c) 2018 Synopsys, Inc
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Synopsys, Inc - initial implementation and documentation
*******************************************************************************/

package com.synopsys.protecode.sc.jenkins;

/**
 * A convenient location to store variables which can easily be found and edited 
 */
public class Configuration {
  /** 
   * The maximum simultaneous requests to Protecode SC backend. Current maximum is 4, more requests 
   * will throw 503, "service unavailable" sometimes
   */
  public static final int MAX_REQUESTS_TO_PROTECODE = 4;
  
  /**
   * Directory to store json reports for scans
   */
  public static final String REPORT_DIRECTORY = "reports";
  
  /**
   * Connection read/write timeout.
   */
  public static final int TIMEOUT_SECONDS = 5000;
}
