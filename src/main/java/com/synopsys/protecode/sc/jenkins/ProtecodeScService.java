/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.types.Types;
import retrofit2.Call;
import retrofit2.http.*;

/**
 *
 * @author pajunen
 */
public interface ProtecodeScService {
   
    @PUT("/api/upload/{filename}")
    public Call<Types.ScanId> scan(@Path("filename") String filename, @Body byte[]bytes);
    
    @POST("/api/product/{product_id}/abort")
    public Call<Types.Meta> abortScan(@Path("product_id") Types.ScanId scanId);
//    
//    @GET("/api/product/{id}")
//    public Call<Types.Result> analysisResult(@Path("id") Types.ScanId scanId);
//    
//    @GET("/api/product/{id}/infoleak")
//    public Call<Types.Infoleak> infoleak(@Path("id") Types.ScanId scanId);
//    
//    @DELETE("/api/product/{id}")
//    public Call<Types.Meta> deleteResult(@Path("id") Types.ScanId scanId);
//    
//    @DELETE("/api/product/{id}/remove")
//    public Call<Types.Meta> deleteFiles(@Path("id") Types.ScanId scanId);   
        
    @GET("/api/status/")
    public Call<Types.Meta> status();
        
    @GET("/api/apps/")
    public Call<Types.Groups> apps();
}
