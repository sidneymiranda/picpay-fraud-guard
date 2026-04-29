package com.github.sidneymiranda.fraudguard.accountservice.application.dto;

import com.github.sidneymiranda.fraudguard.accountservice.domain.AccountType;

/**
 * DTO de entrada para o caso de uso {@code RegisterAccount} (RF-01).
 *
 * <p>Record imutável sem lógica — apenas carregador de dados.
 * Validações de formato e negócio são responsabilidade do domínio.
 *
 * @param fullName    nome completo do titular
 * @param cpf         CPF (com ou sem formatação — sanitizado pelo domínio)
 * @param email       e-mail do titular
 * @param password    senha em texto plano — repassada ao Keycloak, nunca armazenada
 * @param accountType tipo da conta; {@code null} resulta em {@code AccountType.PERSONAL}
 */
public record RegisterAccountRequest(
        String fullName,
        String cpf,
        String email,
        String password,
        AccountType accountType
) {}

