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

import com.google.gson.annotations.SerializedName;
import com.blackduck.protecode.sc.jenkins.exceptions.ApiException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.*;
import lombok.Data;

public final class HttpTypes {

  // Don't instantiate this
  private HttpTypes() {
  }

  /**
   * Response for the call to /api/fetch Response for the call to /api/upload/FILENAME
   */
  public static @Data
  class UploadResponse {

    private Meta meta;
    private ScanState results;
  }

  public static @Data
  class ScanState {

    private int id;
    private String report_url;
    private String sha1sum;
    /**
     * Can be R(eady) B(usy) F(ailed)
     */
    private String status;
    private int product_id;
    private String fileName;
  }

  public static @Data
  class ScanResultResponse {

    private Meta meta;
    private Results results;
  }

  public static @Data
  class Results {

    private String code;
    private int id;
    private String sha1sum;
    private Summary summary;
    private Collection<Component> components;
    private String status;
    private String report_url;
    private Details details;
  }

  public static @Data
  class Group {

    private final int id;
    private final String name;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public static @Data
  class Groups {

    private final Meta meta;
    private final Group[] groups;
  }

  public static @Data
  class Meta {

    private final int code;
  }

  public static @Data
  class Product {

    private final int id;
    private final String name;
    private final int product_id;
    private final Object custom_data;
    private final String sha1sum;
    private final String status;
  }

  public static @Data
  class InfoLeak {

    private final Meta meta;
  }

  public static @Data
  class Summary {

    private Verdict verdict;
    @SerializedName("vuln-count")
    private VulnCount vulnCount;
  }

  public static @Data
  class Verdict {

    private String detailed;
    @SerializedName("short")
    private String shortDesc;
  }

  public static @Data
  class VulnCount {

    private Long total;
    private Long exact;
    private Long historical;
  }

  public static @Data
  class FileData {

    private List<String> fullpath;  // the item at index 1 is the file name
  }

  public static @Data
  class Component {

    @SerializedName("extended-objects")
    private Collection<FileData> files;
    private License license;
    private Collection<String> tags;
    private Collection<VulnContext> vulns;
    private String version;
    private String lib;
    @SerializedName("vuln-count")
    private VulnCount vulnCount;
    @SerializedName("custom_version")
    private String customVersion;
    //private String subcomponent;

    /**
     * Convenience for use when parsing the result.
     *
     * @return The files including the component
     */
    public List<String> getFileNames() {
      List<String> names = new ArrayList<>();
      for (FileData file : files) {
        // The filename is in the index 1
        // TODO: Check that the order is always correct.
        names.add(file.fullpath.get(1));
      }
      return names;
    }
  }

  public static @Data
  class VulnContext {

    private boolean exact;
    private Vuln vuln; // has cve
    private Collection<Triage> triage; // check for matching cve
  }

  public static @Data
  class Vuln {

    private String cve;
    private String cvss;
    private String summary;
  }

  public static @Data
  class Triage {

    @SerializedName("vuln_id")
    private String vulnId;
    private String id;
    private String scope;
  }

  public static @Data
  class License {

    private String url;
    private String type;
    private String name;
  }

  public static @Data
  class Details {

    private Map<String, Integer> filetypes;
    private Map<String, List<String>> flagged;
  }

  public static @Data
  class Status {

    private String value;
    private List<String> validValues = Arrays.asList("A", "B", "C");

    // Custom constructor to check value given
    public Status(String state) {
      if (validValues.contains(state)) {
        this.value = state;
      } else {
        throw new ApiException("Incorrect value given as state");
      }
    }

    @Override
    public String toString() {
      switch (value) {
        // TODO use enum
        case "B":
          return "Busy";
        case "R":
          return "Ready";
        case "F":
          return "Fail";
        default:
          throw new ApiException("Incorrect value exists as state");
      }
    }
  }
}
