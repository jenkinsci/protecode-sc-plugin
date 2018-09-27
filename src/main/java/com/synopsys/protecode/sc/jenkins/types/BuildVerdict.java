/** *****************************************************************************
 * Copyright (c) 2018 Synopsys, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Synopsys, Inc - initial implementation and documentation
 ****************************************************************************** */
package com.synopsys.protecode.sc.jenkins.types;

import lombok.*;

/**
 * A collect all class for protecode scan result/build status
 * @author rukkanen
 */
public @Data class BuildVerdict {
  private final boolean failOnVulns;
 
  private boolean zippingUsed = false;
  private int filesFound = 0;
  private boolean filesWithUntriaVulns = false;
  
  public BuildVerdict(boolean failOnVulns) {
    this.failOnVulns = failOnVulns;
  }
  
  /**
   * @return True if there are no untriaged vulnerabilities
   */
  public boolean verdict() {
    return !filesWithUntriaVulns;
  }
  
  public String verdictStr() {
    String verdict = null;
    if (filesFound == 0) {
      verdict = "No files were found to be scanned.";
    } else if(filesWithUntriaVulns) {
      verdict = "Files with vulnerabilities found.";
    }
    return verdict;
  }
}
