package org.example.devpg.global.exception;

public class RetryableException extends RuntimeException {
    public RetryableException(String m) {
        super(m);
    }
}
