package com.synopsys.protecode.sc.jenkins.types;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.*;

public final class Types {
    
    // Don't instantiate this
    private Types(){
    }       
    
    public static @Data class ScanId {        
        private final int id;                                    
    }

    public static @Data class Meta {
        private final int code;  
    }
    
    public static @Data class Groups {
        private final Meta meta;
        private final Product[] products;                       
    }
    
    public static @Data class Product {
        private final int id;
        private final String name;
        private final int product_id;
        private final Object custom_data;
        private final String sha1sum;
        private final String status;
    }
    
    public static @Data class InfoLeak {       
        private final Meta meta;
    }

    public static @Data class Result {
        private Integer id;
        private String sha1sum;
        private Summary summary;
        private Collection<Component> components;
        private Status status;
        private String report_url;
        private Details details;
    }    

    
    public static @Data class Summary {
        private Verdict verdict;    
        // TODO: naming! vuln-count
        private VulnCount vulnCount;
    }

    public static @Data class Verdict {
        private String detailed;
        // TODO: naming! short
        private String shortDesc;
    }

    public static @Data class VulnCount {
        private Long total;
        private Long exact;
        private Long historical;        
    }

    public static @Data class Component {
        private License license;
        private Collection<String> tags;
        private Collection<Vulns> vulns;
        private String version;
        private String lib;
        // TODO: naming! vuln-count
        private VulnCount vulnCount;
        // TODO: naming! custom_version
        private String customVersion;
        private String subcomponent;       
    }

    public static @Data class Vulns {
        private boolean exact;
        private Vuln vuln;        
    }

    public static @Data class Vuln {
        private String cve;
        private String cvss;
        private String summary;
    }

    public static @Data class License {
        private String url;
        private String type;
        private String name;
    }

    public static @Data class Details {
        private Map<String, Integer> filetypes;
        private Map<String, List<String>> flagged;
    }

    public static @Data class Filetype {
        Map<String, Integer> val;
    }

    public static @Data class Flagged {
        Map<String, List<String>> val;
    }
    
    @Data
    public static enum Status {
        B("Busy"), R("Ready"), F("Fail");

        private String value;

        private Status(String value) {
            this.value = value;
        }

//        @Override
//        public String toString() {
//            return value;
//        }
    }
}
