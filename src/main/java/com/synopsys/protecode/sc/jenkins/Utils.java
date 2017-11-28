/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pajunen
 */
public class Utils {
    
    private Utils(){
        // don't instantiate...
    }
    
    private static final Logger LOGGER = Logger.getLogger(ProtecodeScService.class.getName());      
    
    public static void log(String toLog) {
        LOGGER.log(Level.ALL, toLog);
    } 
}
