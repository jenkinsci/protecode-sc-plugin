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
  private boolean verdict = false;  
  
  public BuildVerdict(boolean success) {
    this.verdict = verdict;
  }
}
