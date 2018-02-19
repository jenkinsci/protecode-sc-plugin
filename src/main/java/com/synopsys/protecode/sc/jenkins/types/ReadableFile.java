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

import com.synopsys.protecode.sc.jenkins.types.Sha1Sum;
import hudson.FilePath;
import java.io.*;
import java.util.Optional;
import lombok.*;


/**
 * TODO, is this actually needed. Check whether this can be integrated to StreamRequestBody
 *       Though it should be remembered that stream != file.
 */
public class ReadableFile {
  @Getter @Setter private FilePath filePath = null;
  @Getter @Setter private Optional<Sha1Sum> sha1sum = Optional.empty();
  
  public ReadableFile(String path) {
    this.filePath = new FilePath(new File(path));
  }
  
  public ReadableFile (FilePath file){
    this.filePath = file;
  }
  
  public ReadableFile (FilePath file, Sha1Sum sha1sum){
    this.filePath = file;
    this.sha1sum = Optional.ofNullable(sha1sum);
  }
  
  public ReadableFile(File file) {
    this.filePath = new FilePath(file);
  }
  
  public String name() {
    return filePath.getName();
  }
  
  @Override
  public String toString() {
    return filePath.getBaseName();
  }
  
  public InputStream read() throws IOException, InterruptedException {
    return filePath.read();
  }
  
  public boolean isDirectory() throws IOException, InterruptedException {
    return filePath.isDirectory();
  }
}
