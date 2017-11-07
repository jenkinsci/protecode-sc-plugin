package com.synopsys.protecode.sc.jenkins;

import hudson.tasks.Builder;
import java.io.IOException;
import java.net.URL;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import org.kohsuke.stapler.DataBoundConstructor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 *
 * @author pajunen
 */
public class ProtecodeScPlugin extends Builder {
    
    private final String task;
    
    @DataBoundConstructor
    public ProtecodeScPlugin(String task) {
        this.task = task;
    }    
    
    public ProtecodeScService backend(URL url) {
        
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder().addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();

                Request.Builder builder = originalRequest.newBuilder().header("Authorization",
                        Credentials.basic("admin", "adminadminadmin"));

                Request newRequest = builder.build();
                return chain.proceed(newRequest);
            }
        }).build();
        
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(url.toString())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build();

        return retrofit.create(ProtecodeScService.class);
    }
}
