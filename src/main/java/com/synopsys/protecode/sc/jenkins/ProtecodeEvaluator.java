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

import com.synopsys.protecode.sc.jenkins.types.FileResult;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ProtecodeEvaluator {
  private static final Logger LOGGER = Logger.getLogger(ProtecodeEvaluator.class.getName());
  
  /**
   * Evaluates the results. Any vulnerabilities or errors associated to file scans will cause false
   * to be returned.
   * @param result The result for the given scan.
   * @return false if any errors or vulns were found. Otherwise true
   */
  public static boolean evaluate(FileResult result) {
    LOGGER.log(Level.FINER, "Evaluating scan results");
    if (!result.hasError()) {
      LOGGER.log(Level.FINER, result.getFilename() + "has result: " + result.verdict());
      return !result.verdict();
    } else {
      LOGGER.log(Level.FINER, result.getFilename() + "has error: " + result.getError());
      return false;
    }
  }
}
