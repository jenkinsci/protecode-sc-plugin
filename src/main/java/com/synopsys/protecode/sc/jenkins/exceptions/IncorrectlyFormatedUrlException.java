/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins.exceptions;

/**
 *
 * @author pajunen
 */
public class IncorrectlyFormatedUrlException extends RuntimeException {
    public IncorrectlyFormatedUrlException(String message) {
        super(message);
    }
}
