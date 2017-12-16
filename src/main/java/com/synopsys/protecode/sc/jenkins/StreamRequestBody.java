/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

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

public class StreamRequestBody extends RequestBody {
    private final InputStream inputStream;
    private final MediaType contentType;

    public StreamRequestBody(MediaType contentType, InputStream inputStream) {
        if (inputStream == null) throw new NullPointerException("inputStream == null");
        this.contentType = contentType;
        this.inputStream = inputStream;
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
}
