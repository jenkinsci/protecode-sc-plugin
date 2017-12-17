/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins.types;

import com.synopsys.protecode.sc.jenkins.exceptions.MalformedSha1SumException;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.ScanResultResponse;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.UploadResponse;
import lombok.Data;

/**
 *
 * @author pajunen
 */
public class InternalTypes {
    
    public static @Data class FileAndResult {
        private String filename = null;
        private UploadResponse uploadResponse = null;               
        private ScanResultResponse resultResponse = null;
        private boolean resultBeingFetched = false;        
        
        public FileAndResult(String filename, UploadResponse uploadResponse) {
            this.filename = filename;
            this.uploadResponse = uploadResponse;
        }
        
        public int uploadHTTPStatus() {
            return uploadResponse.getMeta().getCode();
        }
        
        public String getState() {
            if (uploadResponse != null) {
                return uploadResponse.getResults().getStatus();
            } else {
                // if we don't know the state, we can assume it's busy until otherwise stated
                // this will cause the logic to ask for it again
                return "B";
            }
        }
        
        /**         
         * @return True if the scan result has been fetched.
         */
        public boolean ready() {
            return resultResponse != null;
        }
        
        public boolean verdict() {
            if (!ready()) {
                throw new RuntimeException("No result received for file: " + this.filename);
            }
            return resultResponse.getResults().getSummary().getVulnCount().getTotal() > 0;
        }                        
         
        public SerializableResult getSerializableResult() {
            return new SerializableResult(filename, resultResponse.getResults(), uploadResponse.getMeta());
        }
    }
    
    public static @Data class SerializableResult {
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
    
    /**
     * Must be 40 characters long
    * "sha1sum": "3fcdbdb04baa29ce695ff36af81eaac496364e82"
    */
    public static @Data class Sha1Sum {
        private final String sha1sum;

        public Sha1Sum(String sum) {
            // TODO: add regex for this.
            if (sum.length() == 40) {
                sha1sum = sum;
            } else {
                throw new MalformedSha1SumException("incorrect length of sha1sum, "
                    + "must be 40 characters long");
            }
        }

        @Override
        public String toString() {
            return sha1sum;
        }
    }
    
    public static @Data class Group {
        private String name;
    }
}
