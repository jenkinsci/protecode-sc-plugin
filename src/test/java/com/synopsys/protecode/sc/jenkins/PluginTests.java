/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.interfaces.ProtecodeScService;
import com.synopsys.protecode.sc.jenkins.types.Secret;
import com.synopsys.protecode.sc.jenkins.types.Types;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *
 * @author pajunen
 */
public class PluginTests {
    
    private static final String URL_STRING = "http://localhost:8000";
    
    private static final Logger LOGGER = Logger.getLogger(PluginTests.class.getName());      
    
    private void log(String toLog) {
        LOGGER.log(Level.SEVERE, toLog);
    }
    
    ProtecodeScPlugin plugin = new ProtecodeScPlugin("Bob");    
    
    @Test
    @DisplayName("bob")
    public void testSomething(){
        ProtecodeScService service = 
            ProtecodeScConnection.backend(URL_STRING, "admin", new Secret("adminadminadmin"));
        
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
