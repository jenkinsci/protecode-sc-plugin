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
package com.blackducksoftware.protecode.sc.jenkins.types;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Data;
import retrofit2.Response;

public @Data
class ConnectionStatus {

  private static final Logger LOGGER = Logger.getLogger(ConnectionStatus.class.getName());

  private Response response;
  private Optional<String> error = Optional.empty();

  public ConnectionStatus(Response response) {
    LOGGER.log(Level.FINE, "Connection status: {0}", response.toString());
    this.response = response;
    if (!response.isSuccessful()) {
      try {
        error = Optional.of(response.errorBody().string());
      } catch (IOException ex) {
        error = Optional.of("No error set in failed connection status check.");
      }
    }
  } // No throw, since this is a storage class. It's not its job to inform anything.

  public ConnectionStatus(IOException exception) {
    error = Optional.ofNullable(exception.getMessage());
  }

  public int code() {
    return response.code();
  }

  public boolean ok() {
    return !error.isPresent();
  }

}
