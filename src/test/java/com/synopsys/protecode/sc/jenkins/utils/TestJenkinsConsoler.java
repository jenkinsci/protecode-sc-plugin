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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// TODO: move to junit 5
public class TestJenkinsConsoler {
  private JenkinsConsoler consoler = null;

  @BeforeEach // TODO: perhaps only beforeAll
  void initEach() {
    TaskListener listener = new TaskListenerStub();
    consoler = new JenkinsConsoler(listener);
  }

  @Test
  @DisplayName("Test getCharacters with nonnull values and one char in sequence to create.")
  void testGetCharactersValidWithOneChar() {
    int length = 10;
    String characters = consoler.getCharacters(length, "_");
    assertEquals("__________", characters);
  }

  @Test
  @DisplayName("Test getCharacters with null values and expect exception.")
  void testGetCharactersWithNullValue() {
    int length = 10;
    NullPointerException exception = assertThrows(
      NullPointerException.class,
      () -> {
        String characters = consoler.getCharacters(length, null);
      }
    );
  }

  @Test
  @DisplayName("Test wrapper with valid values and odd/even lenth message")
  void testWrapperWithValidValues() {
    String wrappedOddMessage = consoler.wrapper("Odd has");
    String wrappedEvenMessage = consoler.wrapper("Even has");
    String originalOdd
      = "|---------------------------------- Odd has -----------------------------------|";
    String originalEven
      = "|---------------------------------- Even has ----------------------------------|";

    assertEquals(originalEven, wrappedEvenMessage, "Even Message was incorrect");
    assertEquals(originalOdd, wrappedOddMessage, "Odd Message was incorrect");
  }
}
