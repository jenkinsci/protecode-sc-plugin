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


import java.util.logging.Logger;
import lombok.Data;
import okhttp3.Headers;

/**
 * Simple network utils class
 */
public @Data class NetworkUtils {
  
  private static final Logger LOGGER = Logger.getLogger(NetworkUtils.class.getName());
  
  /**
   * Checks the headers to determine whether protecode is up and running 
   * @param headers headers from response
   * @param code the http response code
   * @return true if connection seems to be ok
   */
  public static boolean checkResponse(Headers headers, int code) {
    // TODO: Check if we should check the headers also
    return !Integer.toString(code).startsWith("4");        
  }
  
  
}
