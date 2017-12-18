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
