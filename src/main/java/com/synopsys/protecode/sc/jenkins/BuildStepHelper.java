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

import com.synopsys.protecode.sc.jenkins.types.InternalTypes.ConnectionStatus;
import hudson.model.TaskListener;
import lombok.Data;

/**
 *
 * 
 */
public @Data class BuildStepHelper {
  private final TaskListener listener;
  
  /**
   * Checks connection status for errors. 
   * TODO: The check is too distributed.
   * @param connectionStatus the status object to check
   * @return true for connection ok, otherwise false
   */
  public boolean connectionOk(ConnectionStatus connectionStatus) {
    if (connectionStatus.getError().isPresent()) {
      listener.fatalError("Connection check to Protecode failed: " + connectionStatus.getError().get());
      return false;
    }
    return !Integer.toString(connectionStatus.code()).startsWith("4");
  }
}
