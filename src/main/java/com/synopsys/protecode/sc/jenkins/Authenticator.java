/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import java.io.IOException;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;

/**
 *
 * @author pajunen
 */
public class Authenticator implements Interceptor {
    private final String credentials;

    public Authenticator(/* Provide credentials at runtime */) {
        this.credentials = Credentials.basic("rest_test", "rest_test");
    }

    @Override
    public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Request authenticatedRequest = request.newBuilder()
                    .header("Authorization", credentials).build();
        return chain.proceed(authenticatedRequest);
    }
}
