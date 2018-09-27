/** *****************************************************************************
 * Copyright (c) 2017 Synopsys, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Synopsys, Inc - initial implementation and documentation
 ****************************************************************************** */
package com.synopsys.protecode.sc.jenkins.types;

import hudson.FilePath;
import java.io.IOException;
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
    if (file == null) {
      throw new NullPointerException("Cannot find file specified to read as streambody");
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
      LOGGER.log(
        Level.WARNING,
        "Could not read file size for FilePath object: {0}",
        file.getRemote()
      );
    }
    return size;
  }

  @Override
  public void writeTo(@NonNull BufferedSink sink) {
    /*
    * TODO!
    * If the "unexpected end of stream" problem persists, try:    
    *    
      OutputStream output = sink.outputStream();      
      InputStream input = file.read();

      byte[] bytes = new byte[1024]; // Again an arbitrary number
      int length;
      while ((length = input.read(bytes)) >= 0) {
        output.write(bytes, 0, length);
      }
    * 
    * This is the "dummy" way of doing this, but might help in figuring what happens with really
    * heavy loads
    */
    
    Source source = null;
    // TODO: Study if other amount would be better... This is just a number-out-of-a-hat.
    long writeAmount = 8192L;  // arbitratry nice number.    
    try {      
      source = Okio.source(file.read());
      while (true) {
        try {
          // Do not use writeAll, since it depends on the source(inputstream) knowing how much it
          // still has. In this case it seems the stream doesnt know how much it has and returns 
          // zero.
          sink.write(source, writeAmount);
          sink.flush();
        } catch (IOException e) {
          // Okio throws exception when attempting to read more than there is in a stream. Why we do 
          // not use sink.writeAll() is because it relies on source.available which returns zero due 
          // to lack of implementation or okio/jenkins compatibility
          break;
        }
      }      
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error while sending file. Error message: {0}", e.getMessage());
    } finally {
      // TODO: Figure out that does this also close the file for real. According to the "ownership"
      // of the handle it shouldn't necessarily close it.
      Util.closeQuietly(source);
    }
  }
}
