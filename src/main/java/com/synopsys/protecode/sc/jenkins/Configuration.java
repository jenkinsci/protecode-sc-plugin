/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.types.Secret;
import java.net.URL;
import lombok.*;

/**
 *
 * @author pajunen
 */
public @Data class Configuration {

    private URL host;
    private String group;
    private String userName;
    private Secret password;    
}
