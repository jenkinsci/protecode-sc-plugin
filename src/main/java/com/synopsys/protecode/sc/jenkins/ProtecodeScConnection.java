/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

//import com.cloudbees.plugins.credentials.Credentials;
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


/**
 *
 * @author pajunen
 */
public class ProtecodeScConnection {
    private ProtecodeScConnection() {
        // Don't instantiate me.
    }
    
    private ProtecodeScApi instance;
    
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
            
            String protecodeScUser = credentials.getUsername();
            String protecodeScPass = credentials.getPassword().toString();
            
            System.out.println("Creds: " + credentials.getUsername() + ":" + credentials.getPassword());

            Request.Builder builder = originalRequest.newBuilder().header(
                "Authorization", 
//                credentials.toString()
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
