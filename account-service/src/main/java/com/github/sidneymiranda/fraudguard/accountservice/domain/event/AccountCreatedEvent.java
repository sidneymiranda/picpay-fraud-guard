package com.github.sidneymiranda.fraudguard.accountservice.domain.event;

import com.github.sidneymiranda.fraudguard.accountservice.domain.Account;
import com.github.sidneymiranda.fraudguard.accountservice.domain.AccountType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de domínio imutável — representa o fato de que uma conta foi criada com sucesso (RF-03).
 *
 * <p>Gerado pela camada de aplicação após persistência bem-sucedida da {@link Account}.
 * Publicado no barramento de eventos pelo {@code EventPublisher}.
 *
 * <p>Payload conforme RF-03:
 * <ul>
 *   <li>{@code userId}      — UUID da conta (mesmo ID do provedor de identidade)</li>
 *   <li>{@code email}       — e-mail normalizado</li>
 *   <li>{@code createdAt}   — instant de criação</li>
 *   <li>{@code accountType} — tipo da conta (PERSONAL ou BUSINESS)</li>
 * </ul>
 */
public record AccountCreatedEvent(
        UUID userId,
        String email,
        Instant createdAt,
        AccountType accountType
) {

    /** Compact constructor — valida invariantes do evento. */
    public AccountCreatedEvent {
        Objects.requireNonNull(userId, "userId é obrigatório");
        Objects.requireNonNull(email, "email é obrigatório");
        Objects.requireNonNull(createdAt, "createdAt é obrigatório");
        Objects.requireNonNull(accountType, "accountType é obrigatório");
        if (email.isBlank()) {
            throw new IllegalArgumentException("email não pode ser vazio");
        }
    }

    /**
     * Factory method — constrói o evento a partir da entidade {@link Account} já persistida.
     *
     * @param account entidade salva com todos os campos preenchidos
     * @return evento pronto para publicação
     */
    public static AccountCreatedEvent from(Account account) {
        return new AccountCreatedEvent(
                account.getId(),
                account.getEmail().value(),
                account.getCreatedAt(),
                account.getAccountType()
        );
    }
}
