/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins.types;

import com.synopsys.protecode.sc.jenkins.exceptions.MalformedSha1SumException;
import lombok.Data;

/**
 * "sha1sum": "3fcdbdb04baa29ce695ff36af81eaac496364e82"
 * @author pajunen
 */
public @Data class Sha1Sum {
    private String sha1sum;

    public Sha1Sum(String sum) {
        // TODO: add regex for this.
        if (sum.length() == 40) {
            sha1sum = sum;
        } else {
            throw new MalformedSha1SumException("incorrect length of sha1sum, "
                + "must be 40 characters long");
        }
    }
    
    public String toString() {
        return sha1sum;
    }
}
