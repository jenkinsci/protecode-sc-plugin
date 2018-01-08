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
package com.synopsys.protecode.sc.jenkins.interfaces;

import com.synopsys.protecode.sc.jenkins.types.HttpTypes;

/**
 * TODO: Add error setters for all interfaces
 */
public class Listeners {
    public static interface ScanService {
        public void processUploadResult(HttpTypes.UploadResponse result);
        public void setError(String reason);
    }       
    
    public static interface PollService {
        public void setScanStatus(HttpTypes.UploadResponse status);
        public void setError(String reason);
    }
    
    public static interface ResultService {
        public void setScanResult(HttpTypes.ScanResultResponse result);
        public void setError(String reason);
    }
    
    public static interface GroupService {
        public void setGroups(HttpTypes.Groups groups);
    }
}
