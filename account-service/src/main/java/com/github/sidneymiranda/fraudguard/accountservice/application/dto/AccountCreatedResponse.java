package com.github.sidneymiranda.fraudguard.accountservice.application.dto;

import java.util.UUID;

/**
 * DTO de saída do caso de uso {@code RegisterAccount}.
 * Retorna apenas o ID da conta criada — conforme especificado nas considerações de implementação.
 *
 * @param id UUID da conta recém-criada (mesmo ID gerado pelo provedor de identidade)
 */
public record AccountCreatedResponse(UUID id) {}

