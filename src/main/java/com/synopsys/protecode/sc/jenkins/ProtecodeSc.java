/*******************************************************************************
* Copyright (c) 2016 Synopsys, Inc
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Synopsys, Inc - initial implementation and documentation
*******************************************************************************/

package com.synopsys.protecode.sc.jenkins;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProtecodeSc {

    private String artifactName;

    private Results results;

    private Meta meta;

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getArtifactName() {
        return artifactName;
    }

    public Meta getMeta() {
        return meta;
    }

    public Results getResults() {
        return results;
    }

    public static enum Status {
        B("Busy"), R("Ready"), F("Fail");

        private String value;

        private Status(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static class Meta {
        private Integer code;

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public Integer getCode() {
            return code;
        }
    }

    public static class Results {

        private Integer id;

        private String sha1sum;

        private Summary summary;

        private Collection<Component> components;

        private Status status;

        private String report_url;

        private Details details;

        public Summary getSummary() {
            return summary;
        }

        public Collection<Component> getComponents() {
            return components;
        }

        public Integer getId() {
            return id;
        }

        public Status getStatus() {
            return status;
        }

        public String getSha1sum() {
            return sha1sum;
        }

        public String getReport_url() {
            return report_url;
        }

        public Details getDetails() {
            return details;
        }

    }

    public static class Summary {
        private Verdict verdict;
        @JsonProperty("vuln-count")
        private VulnCount vulnCount;

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public Verdict getVerdict() {
            return verdict;
        }

        public VulnCount getVulnCount() {
            return vulnCount;
        }
    }

    public static class Verdict {
        private String detailed;
        @JsonProperty("short")
        private String shortDesc;

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public String getDetailed() {
            return detailed;
        }

        public String getShortDesc() {
            return shortDesc;
        }
    }

    public static class VulnCount {
        private Long total;
        private Long exact;
        private Long historical;

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public Long getTotal() {
            return total;
        }

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public Long getExact() {
            return exact;
        }

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public Long getHistorical() {
            return historical;
        }
    }

    public static class Component {
        private License license;
        private Collection<String> tags;
        private Collection<Vulns> vulns;
        private String version;
        private String lib;
        @JsonProperty("vuln-count")
        private VulnCount vulnCount;
        @JsonProperty("custom_version")
        private String customVersion;
        private String subcomponent;

        public License getLicense() {
            return license;
        }

        public String getSubcomponent() {
            return subcomponent;
        }

        public String getCustomVersion() {
            return customVersion;
        }

        public Collection<String> getTags() {
            return tags;
        }

        public Collection<Vulns> getVulns() {
            return vulns;
        }

        public String getVersion() {
            return version;
        }

        public String getLib() {
            return lib;
        }

        public VulnCount getVulnCount() {
            return vulnCount;
        }
    }

    public static class Vulns {
        private boolean exact;
        private Vuln vuln;

        public boolean isExact() {
            return exact;
        }

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public Vuln getVuln() {
            return vuln;
        }
    }

    public static class Vuln {
        private String cve;
        private String cvss;

        private String summary;

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public String getCve() {
            return cve;
        }

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public String getCvss() {
            return cvss;
        }

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public String getSummary() {
            return summary;
        }
    }

    public static class License {
        private String url;
        private String type;
        private String name;

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public String getUrl() {
            return url;
        }

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public String getType() {
            return type;
        }

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public String getName() {
            return name;
        }

    }

    public static class Details {
        private Map<String, Integer> filetypes;
        private Map<String, List<String>> flagged;

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public Map<String, Integer> getFiletypes() {
            return filetypes;
        }

        public Map<String, List<String>> getFlagged() {
            return flagged;
        }

    }

    public static class Filetype {
        Map<String, Integer> val;

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public Map<String, Integer> getVal() {
            return val;
        }
    }

    public static class Flagged {
        Map<String, List<String>> val;

        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
        public Map<String, List<String>> getVal() {
            return val;
        }
    }

}
