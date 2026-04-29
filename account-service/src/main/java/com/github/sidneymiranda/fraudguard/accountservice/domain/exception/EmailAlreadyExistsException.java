package com.github.sidneymiranda.fraudguard.accountservice.domain.exception;

/**
 * Lançada quando se tenta registrar uma Account com um e-mail já cadastrado (RF-01).
 * Representa a violação da regra de negócio "e-mail único" — checada antes de persistir.
 */
public class EmailAlreadyExistsException extends DomainException {

    public EmailAlreadyExistsException(String email) {
        super("E-mail já cadastrado: " + email);
    }
}

