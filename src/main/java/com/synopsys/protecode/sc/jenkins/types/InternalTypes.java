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

import com.synopsys.protecode.sc.jenkins.UIResources;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.Component;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.ScanResultResponse;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.Triage;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.UploadResponse;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.Vuln;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.VulnContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Data;

public class InternalTypes {

  private static final Logger LOGGER = Logger.getLogger(InternalTypes.class.getName());

  public static @Data class VulnStatus {
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

  /**
   * TODO, this class is an example of null-pointer fest.
   */
  public static @Data class FileAndResult {
    private String filename = null;
    private UploadResponse uploadResponse = null;
    private boolean resultBeingFetched = false;
    private String error = null;

    private ScanResultResponse resultResponse = null;
    private Map<Component, VulnStatus> components = new HashMap<>();

    public FileAndResult(String filename, UploadResponse uploadResponse) {
      this.filename = filename;
      this.uploadResponse = uploadResponse;
    }

    public FileAndResult(String filename, String error) {
      this.filename = filename;
      this.error = error;
    }

    // TODO: This should be a model, this is a bit over the limit what it should have.
    public void setResultResponse(ScanResultResponse resultResponse) {
      this.resultResponse = resultResponse;

      for (Component component : resultResponse.getResults().getComponents()) {
        VulnStatus vulnStatus = new VulnStatus();
        if (!component.getVulns().isEmpty()) { // Component has vulns
          Collection<VulnContext> vulnContexts = component.getVulns();
          for (VulnContext vulnContext : vulnContexts) {
            String vuln_cve = vulnContext.getVuln().getCve();
            if (vulnContext.isExact()) {
              Collection<Triage> triages = vulnContext.getTriage();
              if (triages == null) {
                vulnStatus.addUntriagedVuln(vulnContext.getVuln());
              } else {
                boolean triaged = triages.stream().anyMatch((triage)
                  -> (triage.getVulnId().equals(vuln_cve))
                );
                if (triaged) {
                  vulnStatus.addTriagedVuln(vulnContext.getVuln());
                } else {
                  LOGGER.warning("Found vuln with triages, but no matching cve!");
                }
              }
            } else { // LEAVE THIS, it might be handy
              if (vulnContext.getTriage() != null) {
                LOGGER.log(
                  Level.WARNING,
                  "Component: {0}: exact is false, but it has triages!",
                  component.getLib()
                );
              }
            }
          }
        }
        components.put(component, vulnStatus);
      }      
    }

    private boolean hasUntriagedVulns() {
      return components.values().stream().anyMatch(
        (vulnStatus) -> (vulnStatus.untriagedVulnsCount() > 0)
      );
    }

    public long untriagedVulnsCount() {
      long vulns = 0;
      for(VulnStatus vulnStatus : components.values()) {
        vulns += vulnStatus.untriagedVulnsCount();
      }
      return vulns;
    }

    public long triagedVulnsCount() {
      long vulns = 0;
      for(VulnStatus vulnStatus : components.values()) {
        vulns += vulnStatus.triagedVulnsCount();
      }
      return vulns;
    }

    public boolean hasError() {
      return error != null;
    }

    public int uploadHTTPStatus() {
      return uploadResponse.getMeta().getCode();
    }

    public String getState() {
      // TODO: See if this work quite as designed. This might lead to a situation where we poll forever
      if (uploadResponse != null) {
        return uploadResponse.getResults().getStatus();
      } else {
        // if we don't know the state, we can assume it's busy until otherwise stated
        // this will cause the logic to ask for it again
        if (error == null) {
          return "B";
        } else {
          // there's an error so no point in polling more
          return "R";
        }
      }
    }

    /**
     * @return True if the scan result or error has been fetched.
     */
    public boolean hasScanResponse() {
      return (resultResponse != null || hasError());
    }

    /**
     * @return True if component does not have an error, and has no vulns.
     */
    public boolean verdict() {
      return !hasUntriagedVulns() && !hasError();
    }

    public SerializableResult getSerializableResult() {
      // TODO implement error handling for misbuilt responses                 
      return new SerializableResult(
        filename,
        untriagedVulnsCount(),
        triagedVulnsCount(),
        hasUntriagedVulns() ? UIResources.VULNS : "",
        hasUntriagedVulns() ? UIResources.HAS_VULNS_DETAILED : UIResources.NO_VULNS_DETAILED,
        resultResponse.getResults().getReport_url()
      );
    }
  }

  public static @Data class SerializableResult {
    private final String filename;
    private final long untriagedVulns;
    private final long triagedVulns;
    private final String verdict;
    private final String details;
    private final String reportUrl;
  }

  public static @Data class Group {
    private String name;
  }
}
