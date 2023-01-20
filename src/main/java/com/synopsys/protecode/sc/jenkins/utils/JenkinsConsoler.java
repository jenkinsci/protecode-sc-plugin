/** *****************************************************************************
 * Copyright (c) 2018 Synopsys, Inc
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
import com.synopsys.protecode.sc.jenkins.types.FileResult;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

/**
 * Used to make nice and concise prints to the jenkins build console. This is now a pseudo singleton to reduce
 * amount of effort to use it
 */
public class JenkinsConsoler {

  private static JenkinsConsoler instance;

  public static JenkinsConsoler getInstance() {
    if (instance != null) {
      // One might throw an exception if the log isn't set at this point
      return instance;
    }
    instance = new JenkinsConsoler();
    return instance;
  }

  /**
   * Edit this to choose formatted message format.
   */
  final static int LINE_LENGTH = 80;
  final static String STRING_EDGE_CHAR = "|";

  private PrintStream log;

  public void setStream(PrintStream printStream) {
    this.log = printStream;
  }

  public void log(String line) {
    log.println(wrapper(line));
  }

  public void logPure(String line) {
    log.println(line);
  }

  public void error(String error) {
    log.println("ERROR: " + error);
  }

  /**
   * Print start BDBA Jenkins plugin message TODO: take some nice key-value-pair list instead of this
   * 1990-java-style stuff.
   *
   * @param failIfVulns info to print
   * @param includeSubdirectories info to print
   * @param group for printing the group into which the files are uploaded
   */
  public void start(boolean failIfVulns, boolean includeSubdirectories) {
    log(Configuration.TOOL_NAME + " plugin enabled.");
    if (failIfVulns) {
      log("The build will fail if any untriaged vulnerabilities are found.");
    } else {
      log("The build will NOT fail if vulnerabilities are found.");
    }
    if (includeSubdirectories) {
      log("Including subdirectories");
    }
  }

  public void end() {
    // TODO: Some nice printing
  }

  /**
   * Print a report of the fileResults.
   *
   * @param results The fileresult set from which to print the report
   */
  public void printReportString(List<FileResult> results) {
    StringBuilder report = new StringBuilder();
    log("Following files have vulnerabilities");
    for (FileResult result : results) {
      for (Map.Entry<String, Map<HttpTypes.Component, InternalTypes.VulnStatus>> file : result.getFiles().entrySet()) {
        if (file.getValue().values().stream().anyMatch((vulnStatus) -> (vulnStatus.untriagedVulnsCount() > 0))) {
          report.append("\t").append(file.getKey()).append("\n");
        }
      }
    }
    log.println(report.toString());
  }

  /**
   * Make the string 80 chars long with filler "|--- " in front and " ---|" at the end.
   *
   * TODO: Evaluate if users are annoyed by this. Remove if it makes the log unreadable!
   *
   * @param msg message to wrap
   * @return wrapped message
   */
  private String wrapper(String msg) {
    if (msg.length() > 75) { // lets not be fancy if the line is long
      return msg;
    }
    String fillerStart = (msg.length() % 2 == 0) ? " " : " -";
    // leave 2 for | and 2 for spaces around the message.
    int fillerLength = (LINE_LENGTH - msg.length() - 4) / 2; // might drop 0.5
    String filler = getCharacters(fillerLength, "-");
    StringBuilder wrappedMsg = new StringBuilder(LINE_LENGTH);

    return wrappedMsg.append(STRING_EDGE_CHAR)
      .append(filler)
      .append(" ")
      .append(msg)
      .append(fillerStart)
      .append(filler)
      .append(STRING_EDGE_CHAR).toString();
  }

  /**
   * There really isn't a ready made thing for this.
   *
   * @param length the length of the string of given chars
   * @return A string containing the given amount of the given char.
   */
  private String getCharacters(int length, @NonNull String chars) {
    if ((length % chars.length()) != 0) {
      // TODO: do somethign. Perhaps default to return buffer length...
    }
    StringBuilder outputBuffer = new StringBuilder(length);
    for (int i = 0; i < length; i += chars.length()) {
      outputBuffer.append(chars);
    }
    return outputBuffer.toString();
  }
}
