package com.github.sidneymiranda.fraudguard.accountservice.domain.exception;

public class InvalidCpfException extends DomainException {

    public InvalidCpfException(String cpf) {
        super("CPF inválido: " + cpf);
    }
}

