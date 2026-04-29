package com.github.sidneymiranda.fraudguard.accountservice.domain.exception;

/**
 * Lançada quando se tenta registrar uma Account com um CPF já cadastrado (RF-01).
 * Representa a violação da regra de negócio "CPF único" — checada antes de persistir.
 */
public class CpfAlreadyExistsException extends DomainException {

    public CpfAlreadyExistsException(String cpf) {
        super("CPF já cadastrado: " + cpf);
    }
}

