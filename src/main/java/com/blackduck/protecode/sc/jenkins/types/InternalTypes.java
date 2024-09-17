/** *****************************************************************************
 * Copyright (c) 2017 Black Duck Software, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Black Duck Software, Inc - initial implementation and documentation
 ****************************************************************************** */
package com.blackduck.protecode.sc.jenkins.types;

import com.blackduck.protecode.sc.jenkins.types.HttpTypes.Vuln;
import java.util.ArrayList;
import java.util.logging.Logger;
import lombok.Data;

public class InternalTypes {

  private static final Logger LOGGER = Logger.getLogger(InternalTypes.class.getName());

  public static @Data
  class VulnStatus {

    private ArrayList<Vuln> untriagedVulns = new ArrayList();
    private ArrayList<Vuln> triagedVulns = new ArrayList();

    public void addTriagedVuln(Vuln vuln) {
      triagedVulns.add(vuln);
    }

    public void addUntriagedVuln(Vuln vuln) {
      untriagedVulns.add(vuln);
    }

    public long untriagedVulnsCount() {
      return untriagedVulns.size();
    }

    public long triagedVulnsCount() {
      return triagedVulns.size();
    }
  }

  public static @Data
  class Group {

    private String name;
  }
}
