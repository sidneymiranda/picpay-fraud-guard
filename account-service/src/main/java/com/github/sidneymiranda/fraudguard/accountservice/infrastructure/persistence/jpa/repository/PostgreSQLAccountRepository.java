package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.repository;

import com.github.sidneymiranda.fraudguard.accountservice.domain.Account;
import com.github.sidneymiranda.fraudguard.accountservice.domain.repository.AccountRepository;
import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountEntity;
import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.mapper.AccountPersistenceMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgreSQLAccountRepository implements AccountRepository {

    private final PostgreSQLRepository repository;

    public PostgreSQLAccountRepository(PostgreSQLRepository repository) {
        this.repository = repository;
    }

    /**
     * Persiste a conta sem verificações de unicidade — essas validações são
     * responsabilidade exclusiva do caso de uso ({@code RegisterAccount}).
     * Violações de constraint do banco serão propagadas como {@code DataIntegrityViolationException},
     * capturadas e tratadas pela camada de aplicação.
     */
    @Override
    public Account save(Account account) {
        AccountEntity entity = AccountPersistenceMapper.toEntity(account);
        AccountEntity saved = this.repository.save(entity);
        return AccountPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findById(UUID id) {
        return repository.findByExternalId(id)
                .map(AccountPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findByCpf(String cpf) {
        return repository.findByCpf(cpf)
                .map(AccountPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCpf(String cpf) {
        return repository.existsByCpf(cpf);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }
}
