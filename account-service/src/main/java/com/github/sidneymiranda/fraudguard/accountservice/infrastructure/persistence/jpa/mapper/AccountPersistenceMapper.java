package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.mapper;

import com.github.sidneymiranda.fraudguard.accountservice.domain.Account;
import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountEntity;

import java.util.Objects;

/**
 * Mapper manual entre o domínio {@link Account} e a entidade JPA {@link AccountEntity}.
 *
 * <p>MapStruct foi avaliado e descartado para este caso: a conversão envolve
 * Value Objects com factories ({@code CPF.of()}, {@code Email.of()}) e o método
 * {@code Account.reconstitute()} — lógica que o gerador de código do MapStruct
 * não consegue inferir sem customizações extensas que superariam o custo do mapper manual.
 *
 * <p>Ambos os métodos lançam {@link NullPointerException} para entradas {@code null}
 * seguindo a conveção de "fail-fast" — o chamador nunca deve passar nulo.
 */
public class AccountPersistenceMapper {

    private AccountPersistenceMapper() {}

    /**
     * Converte uma {@link AccountEntity} (camada de infraestrutura) para {@link Account} (domínio).
     *
     * @throws NullPointerException se {@code entity} for {@code null}
     */
    public static Account toDomain(AccountEntity entity) {
        Objects.requireNonNull(entity, "AccountEntity must not be null");
        return Account.reconstitute(
                entity.getExternalId(),
                entity.getFullName(),
                entity.getCpf(),
                entity.getEmail(),
                entity.getAccountType(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Converte um {@link Account} (domínio) para {@link AccountEntity} (camada de infraestrutura).
     * Timestamps são gerenciados por {@code @PrePersist}/{@code @PreUpdate} na entidade —
     * o mapper só propaga os valores já presentes no domínio quando diferentes de {@code null}.
     *
     * @throws NullPointerException se {@code domain} for {@code null}
     */
    public static AccountEntity toEntity(Account domain) {
        Objects.requireNonNull(domain, "Account must not be null");

        AccountEntity entity = new AccountEntity();
        entity.setExternalId(domain.getId());
        entity.setFullName(domain.getFullName());
        entity.setCpf(domain.getCpf().value());
        entity.setEmail(domain.getEmail().value());
        entity.setAccountType(domain.getAccountType());
        // createdAt/updatedAt: propagados se presentes; @PrePersist garante defaults caso null
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
