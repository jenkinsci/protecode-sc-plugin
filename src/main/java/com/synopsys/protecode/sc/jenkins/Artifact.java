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

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Resource
public class Artifact {
    File file;
    public Artifact(File f) {
        this.file = f;
    }
    public String getName() {
        return file.getName();
    }

    public InputStream getData() throws IOException {
        return new FileInputStream(file);
    }

    public long getSize() {
        return file.length();
    }
}
