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
package com.synopsys.protecode.sc.jenkins.utils;

import com.synopsys.protecode.sc.jenkins.Configuration;
import com.synopsys.protecode.sc.jenkins.ProtecodeScPlugin;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jenkins.MasterToSlaveFileCallable;

// TODO: Change this to something like instantiable FileGetter or something. static isn't very nice.
//   We need to move the regexps, logger and such through multiple methods and that's not good.
//   Of course the truly atomic helpers can be left alone.
public final class UtilitiesFile {
  private static final Logger LOGGER = Logger.getLogger(ProtecodeScPlugin.class.getName());

  /**
   * String used as regexp to get all files from directory or directory structure .* stands for
   * anything but newlines.
   *
   * The string is used to make sure of backward compatibility, since it's stored as string not
   * pattern.
   */
  public static final String ALL_FILES_REGEX_STRING = ".*";

  /**
   * Pattern used to get all files from directory or directory structure .* stands for anything but
   * newlines.
   */
  public static final Pattern ALL_FILES_PATTERN = Pattern.compile(".*");

  /**
   * Used in the zip file name as the identifier for the zip file to be made.
   */
  public static final String ZIP_FILE_PREFIX = "jenkins-build-";
  
  private UtilitiesFile() {
    // don't instantiate me...
  }

  /**
   * Returns any produced artifacts for the build.
   *
   * @param run Instance of the build
   * @return List of FilePaths produced as artifacts
   */
  // TODO: CLEAN! And add option to use (w/ option to scan artifacts)
  public static List<FilePath> getArtifacts(Run<?, ?> run) {
    return getArtifacts(run, ALL_FILES_PATTERN);
  }

  /**
   * Returns any produced artifacts for the build.
   *
   * @param run Instance of the build
   * @param pattern Regexp pattern used for including only certain artifacts
   * @return List of FilePaths produced as artifacts
   */
  // TODO: CLEAN! And add option to use (w/ option to scan artifacts)
  public static List<FilePath> getArtifacts(Run<?, ?> run, Pattern pattern) {
    List<FilePath> files = new ArrayList<>();
    List<? extends Run<?, ?>.Artifact> buildArtifacts = run.getArtifacts();
    for (Run<?, ?>.Artifact buildArtifact : buildArtifacts) {
      files.add(new FilePath(buildArtifact.getFile()));
    }
    return files;
  }

  /**
   * Returns files in a directory
   *
   * @param fileDirectory Name of the directory to parse through for files
   * @param workspace The workspace to look for files in
   * @param includeSubdirectories If true the method returns all files from the directory structure.
   * @param pattern Regexp to include only certain files. If all is required use
   * UtilitiesFile.ALL_FILES_PATTERN
   * @param run Jenkins build run instance
   * @param listener Jenkins console
   * @return list of files
   */
  public static Optional<FilePath> getFiles(
    String fileDirectory,
    FilePath workspace,
    boolean includeSubdirectories,
    Pattern pattern,
    Run<?, ?> run,
    TaskListener listener
  ) {
    try {
      FilePath directory = workspace.child(cleanUrl(fileDirectory));
      PrintStream log = listener.getLogger();
      log.println("Looking for files in directory: " + directory);

      return Optional.of(
        packageFiles(
          directory, 
          getFiles(directory, includeSubdirectories, pattern, log),
          cleanJobName(run.getExternalizableId())
        )
      );
    } catch (Exception e) {}
    listener.error("Error while reading files from: " + fileDirectory);
    return Optional.empty();
  }

  /**
   * Returns files in a directory which conform to the pattern
   *
   * @param directoryToSearch The directory to parse through for files
   * @param recurse If true the method returns all files from the directory structure.
   * @param log Jenkins log interface
   * @return Files in the specified directory
   */
  private static List<FilePath> getFiles(
    FilePath directoryToSearch,
    boolean includeSubdirectories,
    Pattern pattern,
    PrintStream log
  ) {
    List<FilePath> filesInFolder = new ArrayList<>();
    try {
      directoryToSearch.list().forEach((FilePath file) -> {
        try {
          if (!file.isDirectory()) {
            // TODO Use ANT syntax
            if (pattern.matcher(file.getName()).matches()) {
              // TODO: Implement sha1sum read for file and set it with readableFile.setSha1Sum(xx)              
              filesInFolder.add(file);
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
   * Method zips files at the location of the first file.
   * 
   * @param directory the directory to make the zip file and the base for all listed files. 
   * @param files List of file paths
   * @param zipFileName Name for the zip file
   * @return the zip file of all files to be 
   * @throws Exception 
   */
  static FilePath packageFiles(
    FilePath directory,
    List<FilePath> files,
    String zipFileName
  ) throws Exception {
    //List<FilePath> zipFiles = new ArrayList<>();
    // TODO simplify
    // ugly, but since we want to perform the invoke at the file location
    FilePath fileLocation = files.get(0);
    FilePath zipFile = fileLocation.act(new MasterToSlaveFileCallable<FilePath>() {
      @Override
      public FilePath invoke(File f, VirtualChannel channel) {
        File zipFile = new File(directory + "/" + ZIP_FILE_PREFIX + zipFileName);        
        try {
          if (zipFile.exists()) {
            if (!zipFile.delete()) {
              throw new RuntimeException("Could not delete old zip file at file location.");
            }
          }
          if (!zipFile.createNewFile()) {
            throw new RuntimeException("Could not create zip file at file location.");
          }
          
          try (
            FileOutputStream dest = new FileOutputStream(zipFile); 
            ZipOutputStream zipOutputStream = new ZipOutputStream(dest)
            ) {
            
            for (FilePath fileToRead : files) {
              zipOutputStream.putNextEntry(
                new ZipEntry(
                  // Remove start of path from zip entry name. No point adding the whole path to the
                  // name of the zip entry
                  fileToRead.getRemote().substring(
                    directory.getRemote().length()
                  )
                )
              );
              
              InputStream input = fileToRead.read();
              
              byte[] bytes = new byte[1024]; // Again an arbitrary number
              int length;
              while ((length = input.read(bytes)) >= 0) {
                zipOutputStream.write(bytes, 0, length);
              }
              
              zipOutputStream.flush();
            }
            zipOutputStream.flush();
          }
        } catch (IOException | InterruptedException e) {
          LOGGER.warning("Exception while zipping file. Files will be sent one-by-one. "
            + "Exception: " + e.getMessage());
        }
        return new FilePath(zipFile);
      }
    });
    return zipFile;
  }

  public static boolean removeFilePackage(
    FilePath zipFile
  ) throws Exception {    
    return zipFile.act(new MasterToSlaveFileCallable<Boolean>() {
       @Override
       public Boolean invoke(File f, VirtualChannel channel) throws IOException {
         return f.delete();         
       }
    });
  }
  
  /**
   * Creates a directory in the specified workspace.
   *
   * @param name The name of the directory to create
   * @param workspace The workspace in which to create the directory
   * @param listener The build listener for logging information and possible errors to build
   * console.
   * @return true if creating the directing was successful.
   * @throws java.io.IOException
   * @throws java.lang.InterruptedException
   */
  public static boolean createRemoteDirectory(
    String name,
    FilePath workspace,
    TaskListener listener
  ) throws InterruptedException, IOException {
    return workspace.child(name).act(new MasterToSlaveFileCallable<Boolean>() {
      @Override
      public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        // mkdirs might be able to create some of the parent dirs, so the return
        if (!f.mkdirs()) {
          LOGGER.warning("Remote directory could not be created.");
          return true;
        } else {
          return false;
        }
      }
    });
  }

  /**
   * Convenience method for reports directory.
   *
   * @param run The run instance of the build
   * @return File object pointing to the
   * @throws java.io.IOException Failure to read/create directory
   * @throws java.lang.InterruptedException Failure to read/create directory
   */
  public static FilePath reportsDirectory(Run<?, ?> run) throws IOException, InterruptedException {
    FilePath filePath = new FilePath(
      new File(
        run.getParent().getRootDir().getAbsolutePath() + "/" + Configuration.REPORT_DIRECTORY
      )
    );
    if (!filePath.isDirectory()) {
      LOGGER.log(Level.WARNING, "Made reports directory to: {0}", filePath.getRemote());
      filePath.mkdirs();
    } else {
      LOGGER.log(Level.WARNING, "Report directory already exists, retuning handle: {0}", filePath.getRemote());
    }
    return filePath;
  }

  /**
   * A convenience method which either returns a pattern which includes all files or a pattern for
   * the given string. If the String object "pattern" is empty then the method will return a pattern
   * which includes all.
   *
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
   * A method to add ./ to the start and / to the end of the address if missing
   *
   * @param givenUrl the url to check
   * @return the url with ./ and / added
   */
  private static String cleanUrl(String givenUrl) {
    String cleanUrl = givenUrl;
    if (!cleanUrl.endsWith("/")) {
      cleanUrl = cleanUrl + "/";
    }
    return cleanUrl;
  }
  
  /**
   * Returns only the first part of the job name. Currently the job name is composed of the job name
   * and the number of the build, e.g. "somejob#7". Protecode SC doesn't accept "#" and the number
   * should not be added
   * 
   * @param jobName the jenkins build name to be cleaned for Protecode SC use
   * @return the first token before #, this is the "normal" job name.
   */
  private static String cleanJobName(String jobName) {
    return new StringTokenizer(jobName, "#").nextToken();
  }
}
