/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.interfaces.Listeners.*;
import com.synopsys.protecode.sc.jenkins.types.Sha1Sum;
import com.synopsys.protecode.sc.jenkins.types.Types;

import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.synopsys.protecode.sc.jenkins.interfaces.ProtecodeScApi;

/**
 * This class implements and encapsulates the interface towards Protecode.
 * 
 * - Put implementation required to make calls here. 
 * - Make/Use Interfaces from interfaces.Listeners to call those interested in results.
 * 
 * @author pajunen
 */
public class ProtecodeScService {

    private static final Logger LOGGER = Logger.getLogger(ProtecodeScService.class.getName());      
    // TODO do something nice here, please
    private void log(String toLog) {
        LOGGER.log(Level.SEVERE, toLog);
    }        
    
    Configuration configuration = Configuration.getInstance();
    
    private ProtecodeScApi service = 
        ProtecodeScConnection.backend(configuration);
                    
    public void scan(String fileName, byte[] file, ScanService listener) {  
        RequestBody body = RequestBody.create(null, file);
        Call<Types.UploadResponse> call = service.scan(configuration.getGroup(), fileName, body);                        
        call.enqueue(new Callback<Types.UploadResponse>() {  
            @Override
            public void onResponse(
                Call<Types.UploadResponse> call, 
                Response<Types.UploadResponse> response
            ) {
                log("onResponse");
                if (response.isSuccessful()) {
                    listener.processUploadResult(response.body());            
                } else {
                    // error response, no access to resource?
                    log("Response was failed: " + response.body());
                }
            }
            @Override
            public void onFailure(Call<Types.UploadResponse> call, Throwable t) {
                // something went completely south (like no internet connection)
                log("Error is: " + t.getMessage());
            }
        });
    }

    public void poll(Integer scanId, PollService listener) {  
        log("scanId: " + scanId);
        Call<Types.UploadResponse> call = service.poll(scanId);                        
        call.enqueue(new Callback<Types.UploadResponse>() {  
            @Override
            public void onResponse(Call<Types.UploadResponse> call, Response<Types.UploadResponse> response) {
                log("onResponse");
                if (response.isSuccessful()) {
                    listener.setScanStatus(response.body());            
                } else {
                    // error response, no access to resource?
                    log("Response was failed: " + response.body());
                }
            }
            @Override
            public void onFailure(Call<Types.UploadResponse> call, Throwable t) {
                // something went completely south (like no internet connection)
                log("Error is: " + t.getMessage());
            }
        });
    }
    
    public void scanResult(Sha1Sum sha1sum, ResultService listener) {        
        log("sha1sum: " + sha1sum);
        Call<Types.ScanResultResponse> call = service.scanResult(sha1sum.toString());                        
        call.enqueue(new Callback<Types.ScanResultResponse>() {  
            @Override
            public void onResponse(Call<Types.ScanResultResponse> call, Response<Types.ScanResultResponse> response) {
                log("onResponse");
                if (response.isSuccessful()) {
                    listener.setScanResult(response.body());            
                } else {
                    // error response, no access to resource?
                    log("Response was failed: " + response.body());
                }
            }
            @Override
            public void onFailure(Call<Types.ScanResultResponse> call, Throwable t) {
                // something went completely south (like no internet connection)
                log("Error is: " + t.getMessage());
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
        Call<Types.Groups> call = service.apps();                        
        call.enqueue(new Callback<Types.Groups>() {  
            @Override
            public void onResponse(Call<Types.Groups> call, Response<Types.Groups> response) {
                if (response.isSuccessful()) {
                    log("Response is: " + response.body());            
                } else {
                    // error response, no access to resource?
                    log("Response was failed: " + response.body());
                }
            }

            @Override
            public void onFailure(Call<Types.Groups> call, Throwable t) {
                // something went completely south (like no internet connection)
                log("Error is: " + t.getMessage());
            }
        });
    }
}
