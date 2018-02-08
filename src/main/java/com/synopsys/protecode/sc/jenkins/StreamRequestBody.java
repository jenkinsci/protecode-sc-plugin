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

import java.io.IOException;
import java.io.InputStream;
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
  private final ReadableFile file;
  private final MediaType contentType;  
  
  private static final Logger LOGGER = Logger.getLogger(StreamRequestBody.class.getName());
  
  public StreamRequestBody(MediaType contentType, ReadableFile file) throws IOException, InterruptedException {   
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
      size = file.getFilePath().length();      
    } catch (IOException | InterruptedException e) {
      // IOE = larger scane failure, IE = Interrupted build?
    }
    return size;
  }
  
  @Override
  public void writeTo(@NonNull BufferedSink sink) {
    InputStream inputStream = null;
    Source source = null;
    LOGGER.info("Reading file.");
    try {
      inputStream = file.read();
      long writeAmount = inputStream.available();
      while (writeAmount != 0) {
        source = Okio.source(inputStream);
        sink.write(source, writeAmount);
        sink.flush();
        writeAmount = inputStream.available();
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error while sending file. Error message: {0}", e.getMessage());
    } finally {      
      try {
        inputStream.close();
      } catch (Exception e) {
        // No action: stream might have not been opened.    
      }
      Util.closeQuietly(source);
    }
  }
}
