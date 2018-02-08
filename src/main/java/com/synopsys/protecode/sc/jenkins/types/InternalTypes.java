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
package com.synopsys.protecode.sc.jenkins.types;

import com.synopsys.protecode.sc.jenkins.exceptions.MalformedSha1SumException;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.ScanResultResponse;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.UploadResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;
import lombok.Data;
import retrofit2.Response;


public class InternalTypes {
  
  private static final Logger LOGGER = Logger.getLogger(InternalTypes.class.getName());
  
  public static @Data class FileAndResult {
    private String filename = null;
    private UploadResponse uploadResponse = null;
    private ScanResultResponse resultResponse = null;
    private boolean resultBeingFetched = false;
    private String error = null;
    
    public FileAndResult(String filename, UploadResponse uploadResponse, String error) {
      this.filename = filename;
      this.uploadResponse = uploadResponse;
      this.error = error;
    }
    
    public FileAndResult(String filename, UploadResponse uploadResponse) {
      this.filename = filename;
      this.uploadResponse = uploadResponse;
    }
    
    public FileAndResult(String filename, String error) {
      this.filename = filename;
      this.error = error;
    }
    
    public boolean hasError() {
      return error != null;
    }
    
    public int uploadHTTPStatus() {
      return uploadResponse.getMeta().getCode();
    }
    
    public String getState() {
      if (uploadResponse != null) {
        return uploadResponse.getResults().getStatus();
      } else {
        // if we don't know the state, we can assume it's busy until otherwise stated
        // this will cause the logic to ask for it again
        if (error == null) {
          return "B";
        } else {
          // there's an error so no point in polling more
          return "R";
        }
      }
    }
    
    /**
     * @return True if the scan result or error has been fetched.
     */
    public boolean hasScanResponse() {
      return (resultResponse != null || hasError());
    }
    
    /**
     * @return True if component does not have an error, and has no vulns.
     */
    public boolean verdict() {
      try {
        return resultResponse.getResults().getSummary().getVulnCount().getExact() == 0;
      } catch (NullPointerException npe) {
        // TODO: USE OPTIONAL. This is referenced so often that it's stupidity not just make sure
        // once and for all.
        return false; 
      }
    }
    
    public SerializableResult getSerializableResult() {
      // TODO implement error handling for misbuilt responses
      return new SerializableResult(filename, resultResponse.getResults(), uploadResponse.getMeta());
    }
  }
  
  public static @Data class SerializableResult {
    public SerializableResult() {
      // left empty
    }
    public SerializableResult(String filename, HttpTypes.Results results, HttpTypes.Meta meta) {
      this.filename = filename;
      this.results = results;
      this.meta = meta;
    }
    private String filename;
    private HttpTypes.Results results;
    private HttpTypes.Meta meta;
  }
  
  public static @Data class Secret {
    private final String string;
    
    public String string() {
      return string;
    }
    
    @Override
    public String toString() {
      return "###########";
    }
  }
  
  /**
   * Must be 40 characters long
   * "sha1sum": "3fcdbdb04baa29ce695ff36af81eaac496364e82"
   */
  public static @Data class Sha1Sum {
    private final String sha1sum;
    
    public Sha1Sum(String sum) {
      // TODO: add regex for this.
      if (sum.length() == 40) {
        sha1sum = sum;
      } else {
        throw new MalformedSha1SumException("incorrect length of sha1sum, "
          + "must be 40 characters long");
      }
    }
    
    @Override
    public String toString() {
      return sha1sum;
    }
  }
  
  public static @Data class Group {
    private String name;
  }
  
  public static @Data class ConnectionStatus {
    private Response response;
    private Optional<String> error = Optional.empty();
    
    public ConnectionStatus(Response response) {
      LOGGER.warning(response.toString());
      this.response = response;
      if (!response.isSuccessful()) {
        try {
          error = Optional.of(response.errorBody().string());
        } catch (IOException ex) {
          // No throw, since this is a storage class. It's not its job to inform anything.          
        }
      }
    }
    
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
}
