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

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.File;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

// TODO: Change this to something like instantiable FileGetter or something. static isn't very nice.
//   We need to moce the regexps, logger and such through multiple methods and that's not good.
//   Of course the truly atomic helpers can be left alone.
public class Utils {
  
  private static final Logger LOGGER = Logger.getLogger(ProtecodeScPlugin.class.getName());
  private static final Pattern ALL_FILES_PATTERN = Pattern.compile(".*");
  
  private Utils(){
    // don't instantiate me...
  }
  
  public static List<ReadableFile> getFiles(String fileDirectory, FilePath workspace, Run<?, ?> run,
    TaskListener listener) throws IOException, InterruptedException {
    PrintStream log = listener.getLogger();
    return getFiles(workspace, fileDirectory, log);
  }    
  
  /**
   * Returns any produced artifacts for the build.
   * @param run Instance of the build
   * @return List of ReadableFiles produced as artifacts
   */
  public static List<ReadableFile> getArtifacts(Run<?, ?> run) {
    // .* stands for anything but newlines
    return getArtifacts(run, ALL_FILES_PATTERN);
  }
  
  /**
   * Returns any produced artifacts for the build.
   * @param run Instance of the build
   * @param pattern Regexp pattern used for including only certain artifacts
   * @return List of ReadableFiles produced as artifacts
   */
  public static List<ReadableFile> getArtifacts(Run<?, ?> run, Pattern pattern) {
    List<ReadableFile> readableFiles = new ArrayList<>();
    List<? extends Run<?, ?>.Artifact> buildArtifacts = run.getArtifacts();
    for (Run<?, ?>.Artifact buildArtifact : buildArtifacts) {
      readableFiles.add(new ReadableFile(buildArtifact.getFile()));
    }
    return readableFiles;
  }
  
  public static  List<ReadableFile> getFiles(
    FilePath workspace,
    String directoryToSearch, 
    PrintStream log
  ) {
    return getFiles(
      workspace.child(directoryToSearch),
      false,
      ALL_FILES_PATTERN,
      log
    );
  }
  /**
   * Returns files in a directory
   * @param directoryToSearch Name of the directory to parse through for files
   * @param recurse If true the method returns all files from the directory structure.
   * @return Files in the specified directory
   */
  private static List<ReadableFile> getFiles(
    FilePath directoryToSearch, 
    boolean recurse, 
    Pattern pattern,
    PrintStream log
  ) {
    List<ReadableFile> filesInFolder = new ArrayList<>();
    try {
      directoryToSearch.list().forEach((FilePath file) -> {
        try {
          if (!file.isDirectory()) {
            filesInFolder.add(new ReadableFile(file));  
          } else if (recurse) {
            filesInFolder.addAll(getFiles(file, recurse, pattern, log));
          }
        } catch (IOException | InterruptedException e) {
          // just ignore, DO NOT throw upwards
        }
      });           
    } catch (IOException | InterruptedException e) {
      // maybe the directory doesn't exist.
      log.print("Error while reading folder: " + directoryToSearch.getName());
    }
    return filesInFolder;
  }
  
  public static String replaceSpaceWithUnderscore(String line) {
    // TODO, use something which is certainly not used in other files. Underscore isn't good.
    // Currently underscore is accepted in protecode SC so it's in use.
    return line.replace(" ", "_");
  }
  
  public static boolean makeDirectory(String name, FilePath workspace, TaskListener listener) {
    PrintStream log = listener.getLogger();
    FilePath jsonReportDirectory = workspace.child("reports");
    try {
      jsonReportDirectory.mkdirs();
      if (!jsonReportDirectory.isDirectory()) {
        log.println("Report directory could not be created.");
        return false;
      }
    } catch (IOException | InterruptedException e) {
      return false;
    }
    return true;
  }
  
  /**
   * A method to add ./ to the start and / to the end of the address if missing
   * @param givenUrl the url to check
   * @return the url with ./ and / added
   */
  private static String cleanUrl(String givenUrl) {
    String cleanUrl = givenUrl;
    if (!cleanUrl.endsWith("/")) {
      cleanUrl = cleanUrl + "/";
    }
    if (!cleanUrl.startsWith("./")) {
      cleanUrl = "./" + cleanUrl;
    }
    return cleanUrl;
  }
}
