/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.types.InternalTypes.Secret;
import java.net.URL;
import lombok.*;

public @Data class Configuration {
    
    private static Configuration instance = null;
  
    private Configuration(URL url, String group, String name, String password) {
        this.host = url;
        this.group = group;
        this.userName = name;
        this.password = new Secret(password);
    }
    
    public static Configuration instantiate(URL url, String group, String name, String password) {
        System.out.println("JAMMA JAMMA: instantiate");
        if (instance == null) {
            instance = new Configuration(url, group, name, password);            
        } 
        return instance;
    }
    
    public static Configuration getInstance() {
        if (instance == null) {
            throw new RuntimeException("Configuration must be instantiated first");
        } 
        return instance;
    }
    
    private URL host;
    private String group;
    private String userName;
    private Secret password;   
}
