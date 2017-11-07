package com.synopsys.protecode.sc.jenkins.types;

import lombok.*;

public final class Types {
    
    // Don't instantiate this
    private Types(){
    }       
    
    public static @Data class ScanId {        
        private final int id;
        public ScanId(int newId) {
            id = newId;
        }                                      
    }

    public static @Data class Meta {
        private final int code;

        public Meta(int code) {
            this.code = code;
        }       
    }
    
    public static @Data class Groups {
        private final Meta meta;
        private final Product[] products;
        
        public Groups(Meta meta, Product[] products){
            this.meta = meta;
            this.products = products;
        }                             
    }
    
    public static @Data class Product {
        private final int id;
        private final String name;
        private final int product_id;
        private final Object custom_data;
        private final String sha1sum;
        private final String status;
        
        public Product(int id, String name, int product_id, Object data, String sha1sum, String status) {
            this.id = id;
            this.name = name;
            this.product_id = product_id;
            this.custom_data = data;
            this.sha1sum = sha1sum;
            this.status = status;
        }
    }
    
    public static @Data class InfoLeak {
        
        private final Meta meta;
        public InfoLeak(Meta meta) {
            this.meta = meta;
        }
    }

//    public class Result {
//        private Integer id;
//
//        private String sha1sum;
//
//        private Summary summary;
//
//        private Collection<Component> components;
//
//        private Status status;
//
//        private String report_url;
//
//        private Details details;
//
//        public Summary summary() {
//            return summary;
//        }
//
//        public Collection<Component> components() {
//            return components;
//        }
//
//        public Integer id() {
//            return id;
//        }
//
//        public Status getStatus() {
//            return status;
//        }
//
//        public String getSha1sum() {
//            return sha1sum;
//        }
//
//        public String getReport_url() {
//            return report_url;
//        }
//
//        public Details getDetails() {
//            return details;
//        }
//    }    
//
//    
//    public static class Summary {
//        private Verdict verdict;
//        @JsonProperty("vuln-count")
//        private VulnCount vulnCount;
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public Verdict verdict() {
//            return verdict;
//        }
//
//        public VulnCount vulnCount() {
//            return vulnCount;
//        }
//    }
//
//    public static class Verdict {
//        private String detailed;
//        @JsonProperty("short")
//        private String shortDesc;
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public String detailed() {
//            return detailed;
//        }
//
//        public String shortDesc() {
//            return shortDesc;
//        }
//    }
//
//    public static class VulnCount {
//        private Long total;
//        private Long exact;
//        private Long historical;
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public Long total() {
//            return total;
//        }
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public Long exact() {
//            return exact;
//        }
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public Long historical() {
//            return historical;
//        }
//    }
//
//    public static class Component {
//        private License license;
//        private Collection<String> tags;
//        private Collection<Vulns> vulns;
//        private String version;
//        private String lib;
//        @JsonProperty("vuln-count")
//        private VulnCount vulnCount;
//        @JsonProperty("custom_version")
//        private String customVersion;
//        private String subcomponent;
//
//        public License license() {
//            return license;
//        }
//
//        public String subcomponent() {
//            return subcomponent;
//        }
//
//        public String customVersion() {
//            return customVersion;
//        }
//
//        public Collection<String> tags() {
//            return tags;
//        }
//
//        public Collection<Vulns> vulns() {
//            return vulns;
//        }
//
//        public String version() {
//            return version;
//        }
//
//        public String lib() {
//            return lib;
//        }
//
//        public VulnCount vulnCount() {
//            return vulnCount;
//        }
//    }
//
//    public static class Vulns {
//        private boolean exact;
//        private Vuln vuln;
//
//        public boolean isExact() {
//            return exact;
//        }
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public Vuln vuln() {
//            return vuln;
//        }
//    }
//
//    public static class Vuln {
//        private String cve;
//        private String cvss;
//        private String summary;
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public String cve() {
//            return cve;
//        }
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public String cvss() {
//            return cvss;
//        }
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public String summary() {
//            return summary;
//        }
//    }
//
//    public static class License {
//        private String url;
//        private String type;
//        private String name;
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public String url() {
//            return url;
//        }
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public String type() {
//            return type;
//        }
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public String name() {
//            return name;
//        }
//
//    }
//
//    public static class Details {
//        private Map<String, Integer> filetypes;
//        private Map<String, List<String>> flagged;
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public Map<String, Integer> fileTypes() {
//            return filetypes;
//        }
//
//        public Map<String, List<String>> flagged() {
//            return flagged;
//        }
//
//    }
//
//    public static class Filetype {
//        Map<String, Integer> val;
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public Map<String, Integer> val() {
//            return val;
//        }
//    }
//
//    public static class Flagged {
//        Map<String, List<String>> val;
//
//        @SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
//        public Map<String, List<String>> val() {
//            return val;
//        }
//    }
//    
//    public static enum Status {
//        B("Busy"), R("Ready"), F("Fail");
//
//        private String value;
//
//        private Status(String value) {
//            this.value = value;
//        }
//
//        @Override
//        public String toString() {
//            return value;
//        }
//    }
}
