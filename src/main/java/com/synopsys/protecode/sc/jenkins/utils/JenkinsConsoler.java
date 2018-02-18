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


public class JenkinsConsoler {
  
  /**
   * Edit this to choose formatted line lengths in console print.
   */
  private final static int LINE_LENGTH = 80;

  private final TaskListener listener;
  
  public JenkinsConsoler(TaskListener listener) {
    this.listener = listener;
  }
  
  public void start() {
    
  }
  
  /**
   * Make the string 80 chars long with filler "|--- " in front and " ---|" at the end.
   * @param msg message to wrap
   * @return wrapped message
   */
  private String wrap(String msg) {        
    int fillerLength = (LINE_LENGTH - msg.length())/2; // might drop 0.5
    StringBuilder wrappedMsg = new StringBuilder(LINE_LENGTH);
    wrappedMsg.append("|")
      .append(getCharacters(fillerLength-1, "-"))
      .append(" ")
      .append(msg)
      .append(" ")
      .append(getCharacters(fillerLength-1, "-"));
    if (msg.length()% 2 != 0) {
      wrappedMsg.append("-");      
    }
    return wrappedMsg.append("|").toString();
  }
  
  /**
   * There really isn't a ready made thing for this. 
   * @param length the length of the string of given chars
   * @return A string containing the given amount of the given char.
   */
  private String getCharacters(int length, String chars) {
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

