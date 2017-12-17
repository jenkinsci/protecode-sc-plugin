/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins.interfaces;

import com.synopsys.protecode.sc.jenkins.types.HttpTypes;

/**
 *
 * @author pajunen
 */
public class Listeners {
    public static interface ScanService {
        public void processUploadResult(HttpTypes.UploadResponse result);
    }       
    
    public static interface PollService {
        public void setScanStatus(HttpTypes.UploadResponse status);
    }
    
    public static interface ResultService {
        public void setScanResult(HttpTypes.ScanResultResponse result);
    }
    
    public static interface GroupService {
        public void setGroups(HttpTypes.Groups groups);
    }
}
