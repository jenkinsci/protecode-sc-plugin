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
package com.blackducksoftware.protecode.sc.jenkins.types;

import com.blackducksoftware.protecode.sc.jenkins.UIResources;
import com.blackducksoftware.protecode.sc.jenkins.types.HttpTypes.Component;
import com.blackducksoftware.protecode.sc.jenkins.types.InternalTypes.VulnStatus;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Data;

/**
 * TODO: This object has an ugly double role. It's
 * both a merge of file metadata and scan result. This isn't very nice and should be divided.
 */
public @Data
class FileResult {

  private static final Logger LOGGER = Logger.getLogger(FileResult.class.getName());

  private String filename = null;
  private HttpTypes.UploadResponse uploadResponse = null;
  private boolean resultBeingFetched = false;
  private String error = null;
  private HttpTypes.ScanResultResponse resultResponse = null;
  /**
   * Makes it possible to not parse the result as multiple files in an archive.
   */
  private boolean zippingInUse = false;

  private Map<String, Map<HttpTypes.Component, InternalTypes.VulnStatus>> files = new HashMap<>();

  public FileResult(String filename, HttpTypes.UploadResponse uploadResponse, boolean zippingInUse) {
    this.filename = filename;
    this.uploadResponse = uploadResponse;
    this.zippingInUse = zippingInUse;
  }

  public FileResult(String filename, String error) {
    this.filename = filename;
    this.error = error;
  }

  // TODO: This should be a model, this is a bit over the limit what it should have.
  public void setResultResponse(HttpTypes.ScanResultResponse resultResponse) {
    this.resultResponse = resultResponse;
    if (!zippingInUse) {
      LOGGER.log(Level.WARNING, "Adding filename to result root. No zipping.");
      files.putIfAbsent(this.filename, new HashMap<>());
    }
    for (HttpTypes.Component component : this.resultResponse.getResults().getComponents()) {
      InternalTypes.VulnStatus vulnStatus = new InternalTypes.VulnStatus();
      if (!component.getVulns().isEmpty()) {
        // Component has vulns
        Collection<HttpTypes.VulnContext> vulnContexts = component.getVulns();
        for (HttpTypes.VulnContext vulnContext : vulnContexts) {
          String vuln_cve = vulnContext.getVuln().getCve();
          if (vulnContext.isExact()) {
            Collection<HttpTypes.Triage> triages = vulnContext.getTriage();
            if (triages == null || triages.isEmpty()) {
              vulnStatus.addUntriagedVuln(vulnContext.getVuln());
            } else {
              boolean triaged = triages.stream().anyMatch((triage) -> (triage.getVulnId().equals(vuln_cve)));
              if (triaged) {
                vulnStatus.addTriagedVuln(vulnContext.getVuln());
              } else {
                LOGGER.log(Level.WARNING, "Found vuln with triages, but no matching cve!");
              }
            }
          } else {
            // LEAVE THIS, it might be handy
            if (vulnContext.getTriage() != null) {
              LOGGER.log(Level.WARNING, "Component: {0}: exact is false, but it has triages!", component.getLib());
            }
          }
        }
      }
      if (zippingInUse) {
        LOGGER.log(Level.FINE, "Zipping in use so zipping in result too");
        // Support for multifile packages
        for (String includedFileName : component.getFileNames()) {
          files.putIfAbsent(includedFileName, new HashMap<>());
          files.get(includedFileName).put(component, vulnStatus);
        }
      } else {
        // of course this isn't needed 'as such' but the user expects the zip to be evaluated
        // as a single entity.
        LOGGER.log(Level.FINE, "Making single file/non zip result!");
        files.get(this.filename).put(component, vulnStatus);
      }
      //components.put(component, vulnStatus);
    }
  }

  private boolean hasUntriagedVulns() {
    return files.values().stream().anyMatch(
      (componentMap) -> (componentMap.values().stream().anyMatch(
        (vulnStatus) -> vulnStatus.untriagedVulnsCount() > 0)));
  }

  public long untriagedVulnsCount() {
    long vulns = 0;
    // TODO: Functionalize, though there's a variable outside...
    for (Map<HttpTypes.Component, InternalTypes.VulnStatus> components : files.values()) {
      for (InternalTypes.VulnStatus vulnStatus : components.values()) {
        vulns += vulnStatus.untriagedVulnsCount();
      }
    }
    return vulns;
  }

  public long triagedVulnsCount() {
    long vulns = 0;
    // TODO: Functionalize, though there's a variable outside...
    for (Map<HttpTypes.Component, InternalTypes.VulnStatus> components : files.values()) {
      for (InternalTypes.VulnStatus vulnStatus : components.values()) {
        vulns += vulnStatus.triagedVulnsCount();
      }
    }
    return vulns;
  }

  public boolean errorIsSet() {
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
    return resultResponse != null || errorIsSet();
  }

  /**
   * @return True if component does not have an error, and has no vulns.
   */
  public boolean verdict() {
    return !hasUntriagedVulns() && !errorIsSet();
  }

  public List<SerializableResult> getSerializableResults(int buildNumber) {
    // TODO implement error handling for misbuilt responses
    List<SerializableResult> resultList = new ArrayList<>();

    //LOGGER.fine("result entry set size: " + files.entrySet().size());
    for (Map.Entry<String, Map<Component, VulnStatus>> file : files.entrySet()) {
      long untriagedVulns = 0;
      long triagedVulns = 0;
      // TODO: WET
      for (InternalTypes.VulnStatus vulnStatus : file.getValue().values()) {
        untriagedVulns += vulnStatus.untriagedVulnsCount();
        triagedVulns += vulnStatus.triagedVulnsCount();
      }

      String resultFileName = file.getKey();
      if (!zippingInUse) {
        resultFileName = this.filename;
      }

      resultList.add(
        new SerializableResult(
          resultFileName,
          untriagedVulns,
          triagedVulns,
          untriagedVulns > 0 ? UIResources.VULNS : "",
          untriagedVulns > 0 ? UIResources.HAS_VULNS_DETAILED : UIResources.NO_VULNS_DETAILED,
          resultResponse.getResults().getReport_url(),
          buildNumber
        )
      );
    }
    return resultList;
  }

  public static @Data
  class SerializableResult {

    private final String filename;
    private final long untriagedVulns;
    private final long triagedVulns;
    private final String verdict;
    private final String details;
    private final String reportUrl;
    private final int buildNumber;
  }
}
