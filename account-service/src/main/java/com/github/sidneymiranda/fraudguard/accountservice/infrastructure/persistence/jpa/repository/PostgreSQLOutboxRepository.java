package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.repository;

import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountOutboxEventEntity;
import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountOutboxEventEntity.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Interface JPA — Spring Data gera queries automaticamente (derived queries).
 * Métodos de leitura são para o poller Kafka externo (futuro).
 */
public interface PostgreSQLOutboxRepository extends JpaRepository<AccountOutboxEventEntity, Long> {

    /**
     * Busca eventos pendentes ordenados por criação (FIFO) — usado pelo poller.
     */
    @Query(value = """
        SELECT * FROM account_outbox_event
        WHERE status = :status
        ORDER BY created_at ASC
        LIMIT :limit
    """, nativeQuery = true)
    List<AccountOutboxEventEntity> findPendingEvents(
        @Param("status") String status,
        @Param("limit") int limit
    );

    /**
     * Conta eventos com status específico — para monitoramento.
     */
    long countByStatus(OutboxEventStatus status);
}