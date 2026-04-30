package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.repository;

import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostgreSQLRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByExternalId(UUID externalId);

    Optional<AccountEntity> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);
}
