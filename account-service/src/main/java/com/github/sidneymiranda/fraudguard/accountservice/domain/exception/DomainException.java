package com.github.sidneymiranda.fraudguard.accountservice.domain.exception;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}

