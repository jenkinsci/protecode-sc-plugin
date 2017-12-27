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
package com.synopsys.protecode.sc.jenkins.interfaces;

import com.synopsys.protecode.sc.jenkins.types.HttpTypes;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;


public interface ProtecodeScApi {
    
    //@Headers("Connection: close")
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
