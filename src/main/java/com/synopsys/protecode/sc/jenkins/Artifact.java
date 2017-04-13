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

import hudson.FilePath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Artifact {
    File file;
    FilePath fp;

    public Artifact(File f) {
        this.file = f;
    }

    public Artifact(FilePath fp) {
        this.fp = fp;
    }

    public String getName() {
        if (this.file != null) {
            return file.getName();
        } else if (this.fp != null) {
            return fp.getName();
        }
        return null;
    }

    public InputStream getData() throws IOException, InterruptedException {
        if (this.file != null) {
            return new FileInputStream(file);
        } else if (this.fp != null) {
            return fp.read();
        }
        return null;
    }

    public long getSize() throws IOException, InterruptedException {
        if (file != null) {
            return file.length();
        } else if (this.fp != null) {
            return fp.length();
        }
        return 0;
    }
}
