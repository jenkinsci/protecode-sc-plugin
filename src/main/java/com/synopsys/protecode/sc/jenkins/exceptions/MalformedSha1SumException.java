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
public class MalformedSha1SumException extends RuntimeException {
    public MalformedSha1SumException() { super(); }
    public MalformedSha1SumException(String message) { super(message); }
}
