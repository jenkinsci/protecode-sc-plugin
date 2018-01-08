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

import com.synopsys.protecode.sc.jenkins.interfaces.Listeners.*;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes;
import com.synopsys.protecode.sc.jenkins.interfaces.ProtecodeScApi;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import lombok.Data;

/**
 * This class implements and encapsulates the interface towards Protecode.
 * 
 * As a service class it won't know what it's calling, so the backend must be provided
 * for it.
 * @author pajunen
 */
public @Data class ProtecodeScService {

    private static ProtecodeScApi backend = null;   
    
    public ProtecodeScService(String credentialsId, URL host, boolean checkCertificate){
        backend = ProtecodeScConnection.backend(credentialsId, host, checkCertificate);
    } 
                    
    public void scan(String group, String fileName, RequestBody requestBody, ScanService listener) {  
        Call<HttpTypes.UploadResponse> call = backend.scan(
            group, 
            Utils.replaceSpaceWithUnderscore(fileName), 
            requestBody
        );                        
        call.enqueue(new Callback<HttpTypes.UploadResponse>() {  
            @Override
            public void onResponse(
                Call<HttpTypes.UploadResponse> call, 
                Response<HttpTypes.UploadResponse> response
            ) {
                if (response.isSuccessful()) {
                    listener.processUploadResult(response.body());            
                } else {
                    try {                        
                        listener.setError("Protecode SC returned error for " + 
                            response.errorBody().string() + " for file: " + fileName);
                    } catch (IOException ex) {                                                
                        listener.setError("Protecode SC returned generic error without error message"
                            + " for file: " + fileName);
                    }
                }
            }
            @Override
            public void onFailure(Call<HttpTypes.UploadResponse> call, Throwable t) {
                // something went completely south (like no internet connection)
                listener.setError("Protecode SC returned error for file scan request: " + fileName + 
                    ": " + t.getLocalizedMessage());                             
            }
        });
    }

    public void poll(Integer scanId, PollService listener) {  
        Call<HttpTypes.UploadResponse> call = backend.poll(scanId);                        
        call.enqueue(new Callback<HttpTypes.UploadResponse>() {  
            @Override
            public void onResponse(Call<HttpTypes.UploadResponse> call, 
                Response<HttpTypes.UploadResponse> response) {
                if (response.isSuccessful()) {
                    listener.setScanStatus(response.body());            
                } else {
                    try {                        
                        listener.setError("Protecode SC returned error for " + 
                            response.errorBody().string() + " for scan id: " + scanId);
                    } catch (IOException ex) {                        
                        listener.setError("Protecode SC returned generic error without error message "
                            + "for scan id: " + scanId);
                    }
                }
            }
            @Override
            public void onFailure(Call<HttpTypes.UploadResponse> call, Throwable t) {
                listener.setError("Poll request returned with error for scan id: " + scanId + ". Error was: " + t.getLocalizedMessage());
            }
        });
    }
    
    public void scanResult(String sha1sum, ResultService listener) {        
        Call<HttpTypes.ScanResultResponse> call = backend.scanResult(sha1sum);                        
        call.enqueue(new Callback<HttpTypes.ScanResultResponse>() {  
            @Override
            public void onResponse(Call<HttpTypes.ScanResultResponse> call, Response<HttpTypes.ScanResultResponse> response) {
                if (response.isSuccessful()) {
                    listener.setScanResult(response.body());            
                } else {
                    // error response, no access to resource?
                }
            }
            @Override
            public void onFailure(Call<HttpTypes.ScanResultResponse> call, Throwable t) {
                // something went completely south (like no internet connection)
            }
        });
    }  
    
    /**
     * 
     * @param listener 
     */
    public void groups(GroupService listener) {                
        Call<HttpTypes.Groups> call = backend.groups();
        call.enqueue(new Callback<HttpTypes.Groups>() {  
            @Override
            public void onResponse(Call<HttpTypes.Groups> call, Response<HttpTypes.Groups> response) {
                if (response.isSuccessful()) {
                    listener.setGroups(response.body());          
                } else {
                    // error response, no access to resource?
                    // TODO: Should we handle this somehow
                }
            }

            @Override
            public void onFailure(Call<HttpTypes.Groups> call, Throwable t) {
                // something went completely south (like no internet connection)
                // TODO: Should we handle this somehow
            }
        });
    }
}
