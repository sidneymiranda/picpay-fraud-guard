package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity;

import com.github.sidneymiranda.fraudguard.accountservice.domain.AccountType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA que representa a projeção persistida de {@link com.github.sidneymiranda.fraudguard.accountservice.domain.Account}.
 *
 * <p>Decisões de modelagem:
 * <ul>
 *   <li>{@code internalId} (BIGSERIAL) — PK técnica, nunca exposta; ideal para joins internos e FK de performance.</li>
 *   <li>{@code externalId} (UUID) — ID de negócio, gerado pelo provedor de identidade; exposto via API.</li>
 *   <li>CPF e e-mail possuem {@code unique} via {@code @Table(uniqueConstraints)} + índices explícitos para controle total do DDL.</li>
 *   <li>{@code fullName} NÃO é único — homônimos são legítimos.</li>
 *   <li>Timestamps gerenciados via {@code @PrePersist}/{@code @PreUpdate} — elimina dependência de setar manualmente.</li>
 * </ul>
 */
@Entity
@Table(
    name = "account",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_account_external_id", columnNames = "external_id"),
        @UniqueConstraint(name = "uq_account_cpf",         columnNames = "cpf"),
        @UniqueConstraint(name = "uq_account_email",       columnNames = "email")
    },
    indexes = {
        @Index(name = "idx_account_cpf",         columnList = "cpf"),
        @Index(name = "idx_account_email",       columnList = "email"),
        @Index(name = "idx_account_external_id", columnList = "external_id")
    }
)
public class AccountEntity {

    /** PK técnica — nunca exposta fora da camada de infraestrutura. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long internalId;

    /** ID de negócio — originado no provedor de identidade; imutável após criação. */
    @Column(name = "external_id", nullable = false, updatable = false)
    private UUID externalId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "cpf", length = 11, nullable = false, updatable = false)
    private String cpf;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20, nullable = false)
    private AccountType accountType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // ─── setters ──────────────────────────────────────────────────────────────

    public void setExternalId(UUID externalId) {
        this.externalId = externalId;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ─── getters ──────────────────────────────────────────────────────────────

    public Long getInternalId() {
        return internalId;
    }

    public UUID getExternalId() {
        return externalId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
