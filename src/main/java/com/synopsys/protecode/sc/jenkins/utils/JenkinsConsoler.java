/*******************************************************************************
* Copyright (c) 2018 Synopsys, Inc
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Synopsys, Inc - initial implementation and documentation
*******************************************************************************/

package com.synopsys.protecode.sc.jenkins.utils;

import hudson.model.TaskListener;
import lombok.NonNull;


/**
 * Values left to package visibility for testing.
 */
public class JenkinsConsoler {
  
  /**
   * Edit this to choose formatted message format.
   */
  final static int LINE_LENGTH = 80;
  final static String STRING_EDGE_CHAR = "|";

  private final TaskListener listener;
  
  public JenkinsConsoler(TaskListener listener) {
    this.listener = listener;
  }
  
  /**
   * Print start Protecode SC Jenkins plugin message
   */
  public void start() {
    
  }
  
  /**
   * Make the string 80 chars long with filler "|--- " in front and " ---|" at the end.
   * @param msg message to wrap
   * @return wrapped message
   */
  String wrapper(String msg) {    
    String fillerStart = (msg.length()% 2 == 0) ? " " : " -";
    // leave 2 for | and 2 for spaces around the message.
    int fillerLength = (LINE_LENGTH - msg.length() - 4)/2; // might drop 0.5
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
   * @param length the length of the string of given chars
   * @return A string containing the given amount of the given char.
   */
  String getCharacters(int length, @NonNull String chars) {
    if ((length % chars.length()) != 0) {
      // TODO: do somethign. Perhaps trunk to return buffer length...
    }
    StringBuilder outputBuffer = new StringBuilder(length);
    for (int i = 0; i < length; i+=chars.length()){
       outputBuffer.append(chars);
    }
    return outputBuffer.toString();
  }
} 

