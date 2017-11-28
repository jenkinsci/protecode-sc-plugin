/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins.interfaces;

import com.synopsys.protecode.sc.jenkins.types.HttpTypes;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

/**
 *
 * @author pajunen
 */
public interface ProtecodeScApi {
    @PUT("/api/upload/{filename}")
    public Call<HttpTypes.UploadResponse> scan(
        @Header("Group") String groupName, 
        @Path("filename") String filename, 
        @Body RequestBody bytes
    );   
      
    @GET("/api/product/{id}/")
    public Call<HttpTypes.UploadResponse> poll(@Path("id") int scanId);
    
    @GET("/api/product/{sha1sum}/")
    public Call<HttpTypes.ScanResultResponse> scanResult(@Path("sha1sum") String sha1sum);
   
    @GET("/api/product/{id}/infoleak")
    public Call<HttpTypes.InfoLeak> infoleak(@Path("id") int scanId);
    
    @DELETE("/api/product/{id}")
    public Call<HttpTypes.Meta> deleteResult(@Path("id") int scanId);
    
    @DELETE("/api/product/{id}/remove")
    public Call<HttpTypes.Meta> deleteFiles(@Path("id") int scanId);   
        
    @POST("/api/product/{product_id}/abort")
    public Call<HttpTypes.Meta> abortScan(@Path("product_id") int scanId);
    
    @GET("/api/status/")
    public Call<HttpTypes.Meta> status();
        
    @GET("/api/groups/")    
    public Call<HttpTypes.Groups> groups();
}
