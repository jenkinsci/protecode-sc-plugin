/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins.types;

/**
 *
 * @author pajunen
 */
public class Secret {
    private final String string;
    public Secret(String newString) {
        string = newString;
    }
    
    public String string() {
        return string;
    }
    
    @Override
    public String toString() {
        return "###########";
    }
}
