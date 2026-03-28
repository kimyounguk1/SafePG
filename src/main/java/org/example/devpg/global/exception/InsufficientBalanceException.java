package org.example.devpg.global.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String m) {
        super(m);
    }
}
