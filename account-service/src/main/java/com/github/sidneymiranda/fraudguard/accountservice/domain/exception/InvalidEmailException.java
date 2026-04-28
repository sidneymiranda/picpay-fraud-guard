package com.github.sidneymiranda.fraudguard.accountservice.domain.exception;

public class InvalidEmailException extends DomainException {

    public InvalidEmailException(String email) {
        super("E-mail inválido: " + email);
    }
}

