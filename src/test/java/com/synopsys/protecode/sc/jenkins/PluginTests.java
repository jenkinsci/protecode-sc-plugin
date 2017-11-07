/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.types.Types;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import retrofit2.Call;
import retrofit2.Response;

/**
 *
 * @author pajunen
 */
public class PluginTests {
    
    private static final Logger LOGGER = Logger.getLogger(PluginTests.class.getName());      
    
    private void log(String toLog) {
        LOGGER.log(Level.SEVERE, toLog);
    }
    
    ProtecodeScPlugin plugin = new ProtecodeScPlugin("Bob");    
    
    @Test
    @DisplayName("bob")
    public void testSomething(){
        try {
            ProtecodeScService service = plugin.backend(new URL("http://localhost:8000"));
            Call<Types.Groups> call = service.apps();                        

            // Fetch and print a list of the contributors to the library.
            Response<Types.Groups> response = call.execute();
            log("reponse: " + response.body());            
        } catch (Exception ex) {
            Logger.getLogger(PluginTests.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
