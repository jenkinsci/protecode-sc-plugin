/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.cloudbees.plugins.credentials.Credentials;
import com.synopsys.protecode.sc.jenkins.interfaces.Listeners.*;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes;

import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.synopsys.protecode.sc.jenkins.interfaces.ProtecodeScApi;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes.Group;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes.Secret;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes.Sha1Sum;
import java.net.URL;
import lombok.Data;

/**
 * This class implements and encapsulates the interface towards Protecode.
 * 
 * As a service class it won't know what it's calling, so the backend must be provided
 for it.
 * 
 * @author pajunen
 */
public @Data class ProtecodeScService {

    private static ProtecodeScService instance = null;
    private ProtecodeScApi backend = null;
    
    private ProtecodeScService(String username, Secret password, URL host){
        System.out.println("ProtecodeScService()");
        this.backend = ProtecodeScConnection.backend(host, username, password);
    }
    
    public static ProtecodeScService getInstance(String username, Secret password, URL host) {
        System.out.println("ProtecodeScService.getInstance");
        if (instance == null) {
            instance = new ProtecodeScService(username, password, host);
        }
        return instance;
    }       
                    
    public void scan(Group group, String fileName, byte[] file, ScanService listener) {  
        RequestBody body = RequestBody.create(null, file);
        Call<HttpTypes.UploadResponse> call = backend.scan(group.getName(), fileName, body);                        
        call.enqueue(new Callback<HttpTypes.UploadResponse>() {  
            @Override
            public void onResponse(
                Call<HttpTypes.UploadResponse> call, 
                Response<HttpTypes.UploadResponse> response
            ) {
                Utils.log("onResponse");
                if (response.isSuccessful()) {
                    listener.processUploadResult(response.body());            
                } else {
                    // error response, no access to resource?
                    Utils.log("Response was failed: " + response.body());
                }
            }
            @Override
            public void onFailure(Call<HttpTypes.UploadResponse> call, Throwable t) {
                // something went completely south (like no internet connection)
                Utils.log("Error is: " + t.getMessage());
            }
        });
    }

    public void poll(Integer scanId, PollService listener) {  
        Utils.log("scanId: " + scanId);
        Call<HttpTypes.UploadResponse> call = backend.poll(scanId);                        
        call.enqueue(new Callback<HttpTypes.UploadResponse>() {  
            @Override
            public void onResponse(Call<HttpTypes.UploadResponse> call, Response<HttpTypes.UploadResponse> response) {
                Utils.log("onResponse");
                if (response.isSuccessful()) {
                    listener.setScanStatus(response.body());            
                } else {
                    // error response, no access to resource?
                    Utils.log("Response was failed: " + response.body());
                }
            }
            @Override
            public void onFailure(Call<HttpTypes.UploadResponse> call, Throwable t) {
                // something went completely south (like no internet connection)
                Utils.log("Error is: " + t.getMessage());
            }
        });
    }
    
    public void scanResult(Sha1Sum sha1sum, ResultService listener) {        
        Utils.log("sha1sum: " + sha1sum);
        Call<HttpTypes.ScanResultResponse> call = backend.scanResult(sha1sum.toString());                        
        call.enqueue(new Callback<HttpTypes.ScanResultResponse>() {  
            @Override
            public void onResponse(Call<HttpTypes.ScanResultResponse> call, Response<HttpTypes.ScanResultResponse> response) {
                Utils.log("onResponse");
                if (response.isSuccessful()) {
                    listener.setScanResult(response.body());            
                } else {
                    // error response, no access to resource?
                    Utils.log("Response was failed: " + response.body());
                }
            }
            @Override
            public void onFailure(Call<HttpTypes.ScanResultResponse> call, Throwable t) {
                // something went completely south (like no internet connection)
                Utils.log("Error is: " + t.getMessage());
            }
        });
    }  
//    public void scanResult(Sha1Sum sha1sum, ResultService listener) {        
//        log("sha1sum: " + sha1sum);
//        Call<String> call = service.scanResult(sha1sum.toString());                        
//        call.enqueue(new Callback<String>() {  
//            @Override
//            public void onResponse(Call<String> call, Response<String> response) {
//                log("onResponse");
//                if (response.isSuccessful()) {
//                    listener.setScanResult(response.body());            
//                } else {
//                    // error response, no access to resource?
//                    log("Response was failed: " + response.body());
//                }
//            }
//            @Override
//            public void onFailure(Call<String> call, Throwable t) {
//                // something went completely south (like no internet connection)
//                log("Error is: " + t.getMessage());
//            }
//        });
//    } 
    
    public void groups(GroupService listener) {                
        Call<HttpTypes.Groups> call = backend.groups();                        
        call.enqueue(new Callback<HttpTypes.Groups>() {  
            @Override
            public void onResponse(Call<HttpTypes.Groups> call, Response<HttpTypes.Groups> response) {
                if (response.isSuccessful()) {
                    Utils.log("Response is: " + response.body());            
                } else {
                    // error response, no access to resource?
                    Utils.log("Response was failed: " + response.body());
                }
            }

            @Override
            public void onFailure(Call<HttpTypes.Groups> call, Throwable t) {
                // something went completely south (like no internet connection)
                Utils.log("Error is: " + t.getMessage());
            }
        });
    }
}
