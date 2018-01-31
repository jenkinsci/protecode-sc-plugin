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

import com.synopsys.protecode.sc.jenkins.types.InternalTypes;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ProtecodeEvaluator {
  private static final Logger LOGGER = Logger.getLogger(ProtecodeEvaluator.class.getName());
  
  /**
   * 
   * @param results All the results for the given scan.
   * @return false if any vuln was found. Otherwise true
   */
  public static boolean evaluate(
    List<InternalTypes.FileAndResult> results
  ) {
    LOGGER.log(Level.ALL, "Evaluating");
    return !results.stream().anyMatch((fileAndResult) -> {
      if (!fileAndResult.hasError()) {
        LOGGER.log(Level.ALL, fileAndResult.getFilename() + "has result: " + fileAndResult.verdict());
        return !fileAndResult.verdict();
      } else {
        LOGGER.log(Level.ALL, fileAndResult.getFilename() + "has error: " + fileAndResult.getError());
        return false;
      }
    });
  }
}
