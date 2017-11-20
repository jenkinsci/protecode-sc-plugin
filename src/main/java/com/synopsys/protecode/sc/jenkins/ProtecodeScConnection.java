/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.types.Secret;
import java.net.MalformedURLException;
import java.net.URL;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.synopsys.protecode.sc.jenkins.interfaces.ProtecodeScApi;

/**
 *
 * @author pajunen
 */
public class ProtecodeScConnection {
    private ProtecodeScConnection() {
        // Don't instantiate me.
    }
    
    public static ProtecodeScApi backend(String urlString, String username, Secret password) {    
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            // Don't force try-catch, throw runtime instead
            throw new RuntimeException(ex.getMessage());
        }
        return backend(url, username, password);
    }
    
    public static ProtecodeScApi backend(Configuration conf) {
        return backend(conf.getHost(), conf.getUserName(), conf.getPassword());
    }
    
    public static ProtecodeScApi backend(URL url, String username, Secret password) {
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor((Interceptor.Chain chain) -> {
            Request originalRequest = chain.request();
            
            Request.Builder builder = originalRequest.newBuilder().header("Authorization",                
                    Credentials.basic(username, password.string()));
            
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
