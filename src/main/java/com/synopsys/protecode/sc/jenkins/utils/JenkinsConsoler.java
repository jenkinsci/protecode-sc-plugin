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

import hudson.model.TaskListener;
import java.io.PrintStream;
import lombok.NonNull;

/**
 * Used to make nice and concise prints to the jenkins build console.
 */
public class JenkinsConsoler {

  /**
   * Edit this to choose formatted message format.
   */
  final static int LINE_LENGTH = 80;
  final static String STRING_EDGE_CHAR = "|";

  private final TaskListener listener;
  private final PrintStream log;

  public JenkinsConsoler(TaskListener listener) {
    this.listener = listener;
    this.log = listener.getLogger();
  }

  public void log(String line) {
    log.println(wrapper(line));
  }
  
  public void error(String error) {
    
  }

  /**
   * Print start Protecode SC Jenkins plugin message TODO, take some nice key-value-pair list instead of this
   * 1990-java-style stuff.
   *
   * @param failIfVulns info to print
   * @param includeSubdirectories info to print
   */
  public void start(boolean failIfVulns, boolean includeSubdirectories, String group) {
    log("Protecode SC plugin start. Uploading to group " + group);
    if (failIfVulns) {
      log("The build will fail if any untriaged vulnurabilities are found.");
    } else {
      log("The build will NOT fail if vulnurabilities are found.");
    }
    if (includeSubdirectories) {
      log("Including subdirectories");
    }
  }
  
  public void end() {
    
  }

  /**
   * Make the string 80 chars long with filler "|--- " in front and " ---|" at the end.
   *
   * @param msg message to wrap
   * @return wrapped message
   */
  private String wrapper(String msg) {
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
      // TODO: do somethign. Perhaps trunk to return buffer length...
    }
    StringBuilder outputBuffer = new StringBuilder(length);
    for (int i = 0; i < length; i += chars.length()) {
      outputBuffer.append(chars);
    }
    return outputBuffer.toString();
  }
}
