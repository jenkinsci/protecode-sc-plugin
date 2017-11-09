/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.interfaces.Listeners.GroupService;
import com.synopsys.protecode.sc.jenkins.interfaces.Listeners.ScanService;
import com.synopsys.protecode.sc.jenkins.interfaces.ProtecodeScService;
import com.synopsys.protecode.sc.jenkins.types.Types;

import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class implements and encapsulates the interface towards Protecode.
 * 
 * - Put implementation required to make calls here. 
 * - Make/Use Interfaces from interfaces.Listeners to call those interested in results.
 * 
 * @author pajunen
 */
public class ProtecodeSc {

    private static final Logger LOGGER = Logger.getLogger(ProtecodeSc.class.getName());      
    // TODO do something nice here, please
    private void log(String toLog) {
        LOGGER.log(Level.SEVERE, toLog);
    }        
    
    private ProtecodeScService service = 
        ProtecodeScConnection.backend(Configuration.getInstance());
                    
    public void scan(String fileName, byte[] file, ScanService listener) {  
        // TODO, make this contain the whole scan process
        log("scan");
        RequestBody body = RequestBody.create(null, file);
        Call<Types.ScanId> call = service.scan(fileName, body);                        
        call.enqueue(new Callback<Types.ScanId>() {  
            @Override
            public void onResponse(Call<Types.ScanId> call, Response<Types.ScanId> response) {
                log("onResponse");
                if (response.isSuccessful()) {
                    listener.processScanId(response.body());            
                } else {
                    // error response, no access to resource?
                    log("Response was failed: " + response.body());
                }
            }
            @Override
            public void onFailure(Call<Types.ScanId> call, Throwable t) {
                // something went completely south (like no internet connection)
                log("Error is: " + t.getMessage());
            }
        });
    }

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
