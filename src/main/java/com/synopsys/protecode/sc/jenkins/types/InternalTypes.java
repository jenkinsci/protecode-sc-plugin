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

import com.synopsys.protecode.sc.jenkins.types.HttpTypes.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    
    public int untriagedVulnsCount() {
      return untriagedVulns.size();
    }
    
    public int triagedVulnsCount() {
      return triagedVulns.size();
    }
  }
  
  /**
   * TODO, this class is an example of nullpointer fest.
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

      for (Component component: resultResponse.getResults().getComponents()) {
        LOGGER.warning("Component: " + component.getLib());
        VulnStatus vulnStatus = new VulnStatus();
        if (!component.getVulns().isEmpty()) { // Component has vulns
          LOGGER.warning("Component has some vulns: " + component.getLib());
          Collection<VulnContext> vulnContexts = component.getVulns();
          for (VulnContext vulnContext: vulnContexts) {
            String vuln_cve = vulnContext.getVuln().getCve();
            if (vulnContext.isExact()) {
              LOGGER.warning("Component has EXACT vulns: " + component.getLib());
              Collection<Triage> triages = vulnContext.getTriage();
              if (triages == null ) {
                vulnStatus.addUntriagedVuln(vulnContext.getVuln());
                LOGGER.warning(component.getLib() + ": No triage for: " + vulnContext.getVuln().getCve());
              } else {
                boolean triaged = triages.stream().anyMatch((triage) ->
                  (triage.getVulnId().equals(vuln_cve))
                );
                if (triaged) {
                  vulnStatus.addTriagedVuln(vulnContext.getVuln());
                } else {
                  LOGGER.warning("Found vuln with triages, but no matching cve!");
                }
                LOGGER.warning("Triage present: " + component.getLib());
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
      //try {
        //return resultResponse.getResults().getSummary().getVulnCount().getExact() == 0;
        return !hasUntriagedVulns() && !hasError();
      //} catch (NullPointerException npe) {
        // TODO: USE OPTIONAL. This is referenced so often that it's stupidity not just make sure
        // once and for all.
      //  return false;
      //}
    }
    
    public SerializableResult getSerializableResult() {
      // TODO implement error handling for misbuilt responses
      return new SerializableResult(filename, resultResponse.getResults(), uploadResponse.getMeta());
    }

    public Map<String, Map<Component, VulnStatus>> getStorableResult() {
      Map<String, Map<Component, VulnStatus>> map = new HashMap<>();
      map.put(this.filename, components);
      return map;
    }
  }
  
  public static @Data class SerializableResult {
    public SerializableResult() {
      // left empty
    }
    public SerializableResult(String filename, HttpTypes.Results results, HttpTypes.Meta meta) {
      this.filename = filename;
      this.results = results;
      this.meta = meta;
    }
    private String filename;
    private HttpTypes.Results results;
    private HttpTypes.Meta meta;
  }
  
  public static @Data class Secret {
    private final String string;
    
    public String string() {
      return string;
    }
    
    @Override
    public String toString() {
      return "###########";
    }
  }
  
  public static @Data class Group {
   private String name;
 }
}
