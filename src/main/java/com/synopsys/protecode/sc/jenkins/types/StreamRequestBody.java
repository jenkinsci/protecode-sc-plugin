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

import hudson.FilePath;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import lombok.NonNull;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;


public class StreamRequestBody extends RequestBody {
  private final FilePath file;
  private final MediaType contentType;  
  
  private static final Logger LOGGER = Logger.getLogger(StreamRequestBody.class.getName());
  
  public StreamRequestBody(MediaType contentType, FilePath file) throws IOException, InterruptedException {   
    if (file.read() == null) {
      throw new NullPointerException("File inputStream == null");
    }
    this.file = file;
    this.contentType = contentType;
  }
  
  @Nullable
  @Override
  public MediaType contentType() {
    return contentType;
  }
  
  @Override
  public long contentLength() throws IOException {
    long size = 0;
    try {
      size = file.length();      
    } catch (IOException | InterruptedException e) {
      // IOE = larger scane failure, IE = Interrupted build?
    }
    return size;
  }
  
  @Override
  public void writeTo(@NonNull BufferedSink sink) {
    try {
      LOGGER.warning("-----------------filelength: " + file.length());
      BufferedInputStream inputStream = new BufferedInputStream(file.read());
      LOGGER.warning("bob");
      Source source = null;
      try {
        long writeAmount = 8196;
        source = Okio.source(inputStream);
        while (true) {
          try {
            sink.write(source, writeAmount);            
            sink.flush();
          } catch (Exception e) {
            e.printStackTrace();
            LOGGER.warning("end of write");
            break;
          }                              
        }
      LOGGER.warning("bob6");
      } catch (Exception e) {
        LOGGER.warning("bob7");
        LOGGER.log(Level.WARNING, "Error while sending file. Error message: {0}", e.getMessage());
      } finally {
        LOGGER.warning("bob8");
        try {
          LOGGER.warning("bob9");
          inputStream.close();
        } catch (Exception e) {
          LOGGER.warning("bob10");
          // No action: stream might have not been opened.
        }
        LOGGER.warning("bob11");
        Util.closeQuietly(source);
      }        
    } catch (IOException | InterruptedException ioe) {
      LOGGER.log(Level.WARNING, "Serializing pipewriter error: {0}", ioe.getMessage());
    }
  }
}
