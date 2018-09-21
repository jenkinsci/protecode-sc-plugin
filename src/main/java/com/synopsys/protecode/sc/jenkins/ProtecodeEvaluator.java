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
import com.synopsys.protecode.sc.jenkins.types.BuildVerdict;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ProtecodeEvaluator {
  private static final Logger LOGGER = Logger.getLogger(ProtecodeEvaluator.class.getName());
  
  /**
   * Evaluates the results. Any vulnerabilities or errors associated to file scans will cause false
   * to be returned.
   * @param results The results for the given scan.
   * @return false if any errors or vulns were found. Otherwise true
   */
  public static BuildVerdict evaluate(List<FileResult> results) {
    LOGGER.log(Level.FINER, "Evaluating scan results");
    return new BuildVerdict(!results.stream().anyMatch((result) -> {
        if (!result.verdict()) {
          LOGGER.log(Level.FINER, result.getFilename() + " has result: " + result.verdict());
          return true;
        } else {
          LOGGER.log(Level.FINER, result.getFilename() + " has error: " + result.getError());
          return false;
        }
      }
    ));
  }
}
