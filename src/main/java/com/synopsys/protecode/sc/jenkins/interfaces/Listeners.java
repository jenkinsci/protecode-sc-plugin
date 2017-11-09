/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins.interfaces;

import com.synopsys.protecode.sc.jenkins.types.Types;

/**
 *
 * @author pajunen
 */
public class Listeners {
    public static interface ScanService {
        public void processScanId(Types.ScanId result);
    }
   
    public static interface GroupService {
        public void setGroups(Types.Groups groups);
    }
}
