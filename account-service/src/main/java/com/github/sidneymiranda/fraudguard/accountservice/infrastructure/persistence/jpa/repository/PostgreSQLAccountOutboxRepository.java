package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.repository;

import com.github.sidneymiranda.fraudguard.accountservice.domain.repository.AccountOutboxRepository;
import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountOutboxEventEntity;
import org.springframework.stereotype.Repository;

import java.util.Objects;

/**
 * Adaptador — implementa {@link AccountOutboxRepository} usando JPA.
 */
@Repository
public class PostgreSQLAccountOutboxRepository implements AccountOutboxRepository {

    private final PostgreSQLOutboxRepository jpaRepository;

    public PostgreSQLAccountOutboxRepository(PostgreSQLOutboxRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AccountOutboxEventEntity save(AccountOutboxEventEntity event) {
        Objects.requireNonNull(event, "AccountOutboxEventEntity must not be null");
        return jpaRepository.save(event);
    }
}