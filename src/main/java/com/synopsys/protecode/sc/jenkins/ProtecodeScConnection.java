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

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import java.net.MalformedURLException;
import java.net.URL;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.synopsys.protecode.sc.jenkins.interfaces.ProtecodeScApi;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import okhttp3.Credentials;


public class ProtecodeScConnection {
    private ProtecodeScConnection() {
        // Don't instantiate me.
    }
    
    public static ProtecodeScApi backend(String credentialsId, String urlString) {    
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            // Don't force try-catch, throw runtime instead
            throw new RuntimeException(ex.getMessage());
        }
        return backend(credentialsId, url);
    }
    
    public static ProtecodeScApi backend(String credentialsId, URL url) {
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor((Interceptor.Chain chain) -> {
            Request originalRequest = chain.request();
            
            // TODO: Use service
            StandardUsernamePasswordCredentials credentials = CredentialsMatchers
                .firstOrNull(
                        CredentialsProvider.lookupCredentials(
                                StandardUsernamePasswordCredentials.class,
                                Jenkins.getInstance(), ACL.SYSTEM,
                                new HostnameRequirement(url.toExternalForm())),
                        CredentialsMatchers.withId(credentialsId));
            
            // Right now we can't provide credentials "as is" to protecode so we need to extract to
            // contents
            String protecodeScUser = credentials.getUsername();
            String protecodeScPass = credentials.getPassword().toString();                        

            Request.Builder builder = originalRequest.newBuilder().header(
                "Authorization", 
                 Credentials.basic(protecodeScUser, protecodeScPass)
            );
            
            Request newRequest = builder.build();
            return chain.proceed(newRequest);
        }).build();
        
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(url.toString())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build();

        return retrofit.create(ProtecodeScApi.class);
    }
}
