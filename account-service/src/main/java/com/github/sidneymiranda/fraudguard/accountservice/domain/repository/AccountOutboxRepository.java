package com.github.sidneymiranda.fraudguard.accountservice.domain.repository;

import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountOutboxEventEntity;

/**
 * Porta de persistência para eventos do padrão Outbox — camada de domínio.
 *
 * <p>Abstração que permite ao caso de uso {@code RegisterAccount} persistir
 * eventos sem conhecer detalhes de JPA/PostgreSQL. Interface mínima: apenas {@code save()}.
 */
public interface AccountOutboxRepository {

    /**
     * Persiste um evento no outbox atomicamente com a conta.
     *
     * @param event evento Outbox a persistir (nunca nulo)
     * @return evento com ID persistido
     */
    AccountOutboxEventEntity save(AccountOutboxEventEntity event);
}