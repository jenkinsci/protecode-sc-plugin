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

import com.synopsys.protecode.sc.jenkins.exceptions.MalformedSha1SumException;
import java.util.Objects;
import java.util.logging.Logger;
import lombok.Getter;

/**
   * Must be 40 characters long. This might have nice checks but now it doesn't
   * "sha1sum": "3fcdbdb04baa29ce695ff36af81eaac496364e82"
  */
public class Sha1Sum {

  private static final Logger LOGGER = Logger.getLogger(Sha1Sum.class.getName());
  
  /**
   * We will never use the content of the sha1sum as bytes so no point in saving it as the correct
   * type.
   */
  @Getter private final String sha1Sum;  

  public Sha1Sum(String sum) {
    // TODO: add regex for this.
    if (sum.length() == 40) {
      sha1Sum = sum;
    } else {
      LOGGER.warning("Attempted to build a sha1sum with incorrect length. Throwing exception.");
      throw new MalformedSha1SumException("Incorrect length of sha1sum, "
        + "must be 40 bytes long");
    }
  }

  /** 
   * Override of toString which returns only the bytes of the sha1sum as string
   * @return String representation of the bytes of the sha1sum
   */
  @Override
  public String toString() {
    return sha1Sum;
  }    

  /**
   * Compares the bytes of the sha1sum
   * @param o the object to which attempt to compare to
   * @return true if sha1sum bytes are some
   */
  @Override
  public boolean equals(Object o) {
    if (o != null && o instanceof Sha1Sum) {      
      return ((Sha1Sum)o).getSha1Sum().equals(this.getSha1Sum());      
    } else {
      return false;
    }
  }

  /**
   * Hashcode
   * @return objects hashcode
   */
  @Override
  public int hashCode() {
    int hash = 3;
    hash = 79 * hash + Objects.hashCode(this.sha1Sum);
    return hash;
  }
}