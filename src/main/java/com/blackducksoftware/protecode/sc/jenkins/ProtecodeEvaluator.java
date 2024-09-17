/** *****************************************************************************
 * Copyright (c) 2017 Black Duck Software, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Black Duck Software, Inc - initial implementation and documentation
 ****************************************************************************** */
package com.blackducksoftware.protecode.sc.jenkins;

import com.blackducksoftware.protecode.sc.jenkins.types.BuildVerdict;
import com.blackducksoftware.protecode.sc.jenkins.types.FileResult;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProtecodeEvaluator {

  private static final Logger LOGGER = Logger.getLogger(ProtecodeEvaluator.class.getName());

  /**
   * Evaluates the results. Any vulnerabilities or errors associated to file scans will cause false to be
   * returned.
   *
   * @param results The results for the given scan.
   * @param verdict The verdict to further while evaluating the build.
   */
  public static void evaluate(List<FileResult> results, BuildVerdict verdict) {
    LOGGER.log(Level.INFO, "Evaluating scan results");
    boolean hasVulns = results.stream().anyMatch((result) -> {
      if (result.verdict()) {
        LOGGER.log(Level.INFO, result.getFilename() + " has result: " + result.verdict());
        return false;
      } else {
        LOGGER.log(Level.INFO, result.getFilename() + " has error: " + result.getError());
        return true; // has untriaged vulns or an error
      }
    }
    );
    verdict.setFilesWithUntriagedVulns(hasVulns);
  }
}
