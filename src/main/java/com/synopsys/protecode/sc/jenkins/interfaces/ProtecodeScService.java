/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins.interfaces;

import com.synopsys.protecode.sc.jenkins.types.Sha1Sum;
import com.synopsys.protecode.sc.jenkins.types.Types;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

/**
 *
 * @author pajunen
 */
public interface ProtecodeScService {
    @PUT("/api/upload/{filename}")
    public Call<Types.UploadResponse> scan(
        @Header("Group") String groupName, 
        @Path("filename") String filename, 
        @Body RequestBody bytes
    );   
      
    @GET("/api/product/{id}/")
    public Call<Types.UploadResponse> poll(@Path("id") int scanId);
    
    @GET("/api/product/{sha1sum}/")
    public Call<Types.ScanResultResponse> scanResult(@Path("sha1sum") String sha1sum);
   
    @GET("/api/product/{id}/infoleak")
    public Call<Types.InfoLeak> infoleak(@Path("id") int scanId);
    
    @DELETE("/api/product/{id}")
    public Call<Types.Meta> deleteResult(@Path("id") int scanId);
    
    @DELETE("/api/product/{id}/remove")
    public Call<Types.Meta> deleteFiles(@Path("id") int scanId);   
        
    @POST("/api/product/{product_id}/abort")
    public Call<Types.Meta> abortScan(@Path("product_id") int scanId);
    
    @GET("/api/status/")
    public Call<Types.Meta> status();
        
    @GET("/api/apps/")    
    public Call<Types.Groups> apps();
}
