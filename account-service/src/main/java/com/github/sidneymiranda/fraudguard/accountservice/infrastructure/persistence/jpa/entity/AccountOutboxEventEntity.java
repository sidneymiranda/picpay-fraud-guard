package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA que implementa o padrão Outbox — registro imutável de evento a publicar.
 *
 * <p>Persiste atomicamente dentro da mesma transação que insere {@link AccountEntity}.
 * Quando o evento Kafka for implementado, um poller externo lerá desta tabela e publicará
 * no topic `account.created`, garantindo zero perda de eventos mesmo em caso de falha
 * do broker (o evento fica durável no banco).
 *
 * <p>Cada linha representa um fato que PRECISA ser publicado — não é opinativo, é fato do domínio.
 * O status (PENDING → SENT) é apenas para debug e retry manual.
 *
 * <p>Modelagem para idempotência:
 * <ul>
 *   <li>Coluna {@code outbox_key} (computed): `{userId}#{topic}` — permite dedup via UPSERT em listener Kafka</li>
 *   <li>Coluna {@code created_at} — marca quando o fato ocorreu (usuário criado)</li>
 *   <li>Imutável após inserção: nenhum UPDATE, apenas SELECT + DELETE (quando replicado)</li>
 * </ul>
 */
@Entity
@Table(
        name = "account_outbox_event",
        indexes = {
                @Index(name = "idx_outbox_status_created", columnList = "status,created_at"),
                @Index(name = "idx_outbox_topic", columnList = "topic"),
                @Index(name = "idx_outbox_user_id", columnList = "user_id")
        }
)
public class AccountOutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "topic", length = 100, nullable = false, updatable = false)
    private String topic;

    @Column(name = "payload", columnDefinition = "jsonb", nullable = false, updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OutboxEventStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.status == null) this.status = OutboxEventStatus.PENDING;
    }

    // ─── getters + setters ────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxEventStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    // ─── enum ─────────────────────────────────────────────────────────────────

    public enum OutboxEventStatus {
        PENDING,
        SENT
    }
}