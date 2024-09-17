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
package com.blackduck.protecode.sc.jenkins.exceptions;

public class MalformedSha1SumException extends RuntimeException {

  public MalformedSha1SumException() {
    super();
  }

  public MalformedSha1SumException(String message) {
    super(message);
  }
}
