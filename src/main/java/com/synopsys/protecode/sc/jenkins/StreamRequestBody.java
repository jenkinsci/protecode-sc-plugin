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
package com.synopsys.protecode.sc.jenkins;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import lombok.NonNull;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.jenkinsci.remoting.RoleChecker;


public class StreamRequestBody extends RequestBody {
    private final InputStream inputStream;
    private final MediaType contentType;
    // TODO: private final long size;

    public StreamRequestBody(MediaType contentType, ReadableFile file) throws IOException, InterruptedException {
        if (file.read() == null) {
            throw new NullPointerException("inputStream == null");
        }
        this.contentType = contentType;
        this.inputStream = file.read();        
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public long contentLength() throws IOException {
        return inputStream.available() == 0 ? -1 : inputStream.available();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(inputStream);
            sink.writeAll(source);
        } finally {
            Util.closeQuietly(source);
        }
    }
    
    private static final class FileReader implements FilePath.FileCallable<File> {

        @Override
        public void checkRoles(RoleChecker arg0) throws SecurityException {
          // intentionally left empty
        }

        @Override
        public File invoke(File f, VirtualChannel channel)
                throws IOException, InterruptedException {
            return f.getAbsoluteFile();
        }

    }
}
