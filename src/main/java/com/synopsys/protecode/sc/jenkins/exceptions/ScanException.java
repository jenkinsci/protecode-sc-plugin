package com.synopsys.protecode.sc.jenkins.exceptions;

public class ScanException extends RuntimeException {
    public ScanException() { super(); }
    public ScanException(String message) { super(message); }
    public ScanException(Throwable t) { super(t); }
}
