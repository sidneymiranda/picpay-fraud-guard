package com.github.sidneymiranda.fraudguard.accountservice.application.exception;

/**
 * Lançada quando a orquestração do caso de uso {@code RegisterAccount} falha
 * após a compensação SAGA ter sido executada.
 *
 * <p>Indica que houve um erro não-recuperável durante o cadastro (ex: falha de persistência
 * ou publicação de evento), e que o usuário criado no Keycloak já foi removido para
 * garantir consistência distribuída.
 */
public class AccountRegistrationException extends RuntimeException {

    public AccountRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}

