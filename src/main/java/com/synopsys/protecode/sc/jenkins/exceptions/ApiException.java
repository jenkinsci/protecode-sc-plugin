package com.synopsys.protecode.sc.jenkins.exceptions;

public class ApiException extends RuntimeException {

    public ApiException() { super(); }

    public ApiException(String message) { super(message); }

    public ApiException(Throwable t) { super(t); }
}
