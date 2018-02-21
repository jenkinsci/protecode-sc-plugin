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
package com.synopsys.protecode.sc.jenkins.utils;

import com.synopsys.protecode.sc.jenkins.ProtecodeScPlugin;
import com.synopsys.protecode.sc.jenkins.types.ReadableFile;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

// TODO: Change this to something like instantiable FileGetter or something. static isn't very nice.
//   We need to move the regexps, logger and such through multiple methods and that's not good.
//   Of course the truly atomic helpers can be left alone.
public final class UtilitiesFile {  
  private static final Logger LOGGER = Logger.getLogger(ProtecodeScPlugin.class.getName());
  
  /**
   * String used as regexp to get all files from directory or directory structure
   * .* stands for anything but newlines.
   * 
   * The string is used to make sure of backward compatibility, since it's stored as string not 
   * pattern.
   */
  public static final String ALL_FILES_REGEX_STRING = ".*";
  
  /**
   * Pattern used to get all files from directory or directory structure
   * .* stands for anything but newlines.
   */
  public static final Pattern ALL_FILES_PATTERN = Pattern.compile(".*");  
  
  private UtilitiesFile(){
    // don't instantiate me...
  }
  
  /**
   * Returns any produced artifacts for the build.
   * @param run Instance of the build
   * @return List of ReadableFiles produced as artifacts
   */
  public static List<ReadableFile> getArtifacts(Run<?, ?> run) {    
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
  
  /**
   * Returns files in a directory
   * @param fileDirectory Name of the directory to parse through for files
   * @param workspace The workspace to look for files in
   * @param includeSubdirectories If true the method returns all files from the directory structure.
   * @param pattern Regexp to include only certain files. If all is required use UtilitiesFile.ALL_FILES_PATTERN
   * @param run Jenkins build run instance
   * @param listener Jenkins console 
   * @return list of files 
   */
  public static List<ReadableFile> getFiles(
    String fileDirectory, 
    FilePath workspace,
    boolean includeSubdirectories,
    Pattern pattern,
    Run<?, ?> run,
    TaskListener listener
  ) {
    List<ReadableFile> files = new ArrayList<>();
    
    try {
      FilePath directory;
      if (!absolutePath(fileDirectory)) {
        directory = workspace.child(cleanUrl(fileDirectory));
      } else {
        directory = new FilePath(new File(fileDirectory));
      }
      PrintStream log = listener.getLogger();
      log.println("Looking for files in directory: " + directory);
      files = getFiles(directory, includeSubdirectories, pattern, log);
    } catch (Exception e) {
      listener.error("Could not read files from: <workspace>/" + fileDirectory);
    }
    return files;
  }    
  
  /**
   * Returns files in a directory which conform to the pattern
   * @param directoryToSearch 
   * @param recurse 
   * @param log Jenkins log interface
   * @return Files in the specified directory
   */
  private static List<ReadableFile> getFiles(
    FilePath directoryToSearch, 
    boolean includeSubdirectories, 
    Pattern pattern,
    PrintStream log
  ) {
    List<ReadableFile> filesInFolder = new ArrayList<>();
    try {
      directoryToSearch.list().forEach((FilePath file) -> {
        try {
          if (!file.isDirectory()) {
            if (pattern.matcher(file.getName()).matches()) {
              // TODO: Implement sha1sum read for file and set it with readableFile.setSha1Sum(xx)              
              filesInFolder.add(new ReadableFile(file));  
            }
          } else if (includeSubdirectories) {
            filesInFolder.addAll(getFiles(file, includeSubdirectories, pattern, log));
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
  
  /**
   * Creates a directory in the specified workspace.
   * @param name The name of the directory to create
   * @param workspace The workspace in which to create the directory
   * @param listener The build listener for logging information and possible errors to build console.
   * @return true if creating the directing was successful.
   */
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
   * A convenience method which either returns a pattern which includes all files or a pattern for 
   * the given string. If the String object "pattern" is empty then the method will return a pattern
   * which includes all.
   * @param pattern The string representation of the pattern. 
   * @return Either a Pattern object with the given string or a pattern for all
   */
  public static Pattern patternOrAll(String pattern) {
    if (null == pattern) {
      return UtilitiesFile.ALL_FILES_PATTERN;
    }
    return "".equals(pattern) ? UtilitiesFile.ALL_FILES_PATTERN : Pattern.compile(pattern);    
  }
  
  /**
   * Method builds the path to the working directory. 
   * @param filesToScanDirectory
   * @return 
   */
//  public static String directoryToScan(String filesToScanDirectory) {
//    @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
//    return "".equalsfilesToScanDirectory) ? filesToScanDirectory : "";
//  }
  
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
  
  /**
   * Checks whether the path is absolute or only a path "in" the workspace
   * @param path the path to check
   * @return true if path seems to be an absolute path, not a relative path in the workspace
   */
  private static boolean absolutePath(String path) {
    // TODO: Check for windows style path, eg. C: or D:
    return path.startsWith("/");
  }
}
