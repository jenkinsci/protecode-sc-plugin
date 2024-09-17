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
package com.blackducksoftware.protecode.sc.jenkins.types;

import java.util.Optional;
import lombok.Data;

/**
 * A collect all class for protecode scan result/build status
 */
public @Data
class BuildVerdict {

  private final boolean failOnVulns;

  private boolean zippingUsed = false;
  private long filesFound = 0;
  private boolean filesWithUntriagedVulns = false;
  private Optional<String> error = Optional.empty();

  public BuildVerdict(boolean failOnVulns) {
    this.failOnVulns = failOnVulns;
  }

  public void setError(String error) {
    this.error = Optional.of(error);
  }

  /**
   * @return True if there are no untriaged vulnerabilities
   */
  public boolean verdict() {
    return !filesWithUntriagedVulns;
  }

  public String verdictStr() {
    String verdict = null;
    if (this.error.isPresent()) {
      verdict = error.get();
    } else if (filesFound == 0) {
      verdict = "No files were found to be scanned.";
    } else if (filesWithUntriagedVulns) {
      verdict = "Files with vulnerabilities found.";
    } else {
      verdict = "No errors or Vulnerabilities found";
    }
    return verdict;
  }
}
