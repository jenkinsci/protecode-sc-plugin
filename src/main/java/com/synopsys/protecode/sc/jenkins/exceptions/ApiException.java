package com.synopsys.protecode.sc.jenkins.exceptions;

public class ApiException extends Exception {

    public ApiException() { super(); }

    public ApiException(String message) { super(message); }

    public ApiException(Throwable t) { super(t); }
}
