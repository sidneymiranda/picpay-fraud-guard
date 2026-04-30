package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.mapper;

import com.github.sidneymiranda.fraudguard.accountservice.domain.Account;
import com.github.sidneymiranda.fraudguard.accountservice.domain.AccountType;
import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AccountPersistenceMapper — Conversão infraestrutura ↔ domínio")
class AccountPersistenceMapperTest {

    // ─── fixtures ─────────────────────────────────────────────────────────────

    private static final UUID    ID         = UUID.randomUUID();
    private static final String  FULL_NAME  = "Sidney Miranda";
    private static final String  CPF        = "52998224725";
    private static final String  EMAIL      = "sidney@example.com";
    private static final Instant CREATED_AT = Instant.parse("2024-06-01T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2024-06-10T08:30:00Z");

    /** Cria uma {@link AccountEntity} completamente preenchida. */
    private AccountEntity entityCompleta(AccountType type) {
        AccountEntity e = new AccountEntity();
        e.setExternalId(ID);
        e.setFullName(FULL_NAME);
        e.setCpf(CPF);
        e.setEmail(EMAIL);
        e.setAccountType(type);
        e.setCreatedAt(CREATED_AT);
        e.setUpdatedAt(UPDATED_AT);
        return e;
    }

    /** Cria um {@link Account} de domínio via builder. */
    private Account domainCompleto(AccountType type) {
        return Account.builder()
                .id(ID)
                .fullName(FULL_NAME)
                .cpf(CPF)
                .email(EMAIL)
                .accountType(type)
                .createdAt(CREATED_AT)
                .build();
    }

    // ─── toDomain ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toDomain() — AccountEntity → Account")
    class ToDomain {

        @Test
        @DisplayName("lança NullPointerException quando entity é null")
        void deveLancarNpeParaEntityNull() {
            assertThatThrownBy(() -> AccountPersistenceMapper.toDomain(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("AccountEntity must not be null");
        }

        @Test
        @DisplayName("mapeia externalId para Account.id corretamente")
        void deveMapearExternalIdParaId() {
            Account result = AccountPersistenceMapper.toDomain(entityCompleta(AccountType.PERSONAL));
            assertThat(result.getId()).isEqualTo(ID);
        }

        @Test
        @DisplayName("mapeia fullName para Account.fullName corretamente")
        void deveMapearFullName() {
            Account result = AccountPersistenceMapper.toDomain(entityCompleta(AccountType.PERSONAL));
            assertThat(result.getFullName()).isEqualTo(FULL_NAME);
        }

        @Test
        @DisplayName("mapeia CPF como Value Object com os dígitos da entity")
        void deveMapearCpfComoValueObject() {
            Account result = AccountPersistenceMapper.toDomain(entityCompleta(AccountType.PERSONAL));
            assertThat(result.getCpf().value()).isEqualTo(CPF);
        }

        @Test
        @DisplayName("mapeia email como Value Object normalizado (lowercase)")
        void deveMapearEmailComoValueObject() {
            AccountEntity entity = entityCompleta(AccountType.PERSONAL);
            entity.setEmail("SIDNEY@EXAMPLE.COM");

            Account result = AccountPersistenceMapper.toDomain(entity);

            assertThat(result.getEmail().value()).isEqualTo("sidney@example.com");
        }

        @Test
        @DisplayName("mapeia accountType PERSONAL corretamente")
        void deveMapearAccountTypePersonal() {
            Account result = AccountPersistenceMapper.toDomain(entityCompleta(AccountType.PERSONAL));
            assertThat(result.getAccountType()).isEqualTo(AccountType.PERSONAL);
        }

        @Test
        @DisplayName("mapeia accountType BUSINESS corretamente")
        void deveMapearAccountTypeBusiness() {
            Account result = AccountPersistenceMapper.toDomain(entityCompleta(AccountType.BUSINESS));
            assertThat(result.getAccountType()).isEqualTo(AccountType.BUSINESS);
        }

        @Test
        @DisplayName("preserva createdAt da entity no Account restituído")
        void devePreservarCreatedAt() {
            Account result = AccountPersistenceMapper.toDomain(entityCompleta(AccountType.PERSONAL));
            assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
        }

        @Test
        @DisplayName("preserva updatedAt da entity no Account restituído")
        void devePreservarUpdatedAt() {
            Account result = AccountPersistenceMapper.toDomain(entityCompleta(AccountType.PERSONAL));
            assertThat(result.getUpdatedAt()).isEqualTo(UPDATED_AT);
        }

        @Test
        @DisplayName("Account reconstituído não possui campos nulos obrigatórios")
        void resultadoNaoPossuiCamposNulos() {
            Account result = AccountPersistenceMapper.toDomain(entityCompleta(AccountType.PERSONAL));

            assertThat(result.getId()).isNotNull();
            assertThat(result.getFullName()).isNotBlank();
            assertThat(result.getCpf()).isNotNull();
            assertThat(result.getEmail()).isNotNull();
            assertThat(result.getAccountType()).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();
        }
    }

    // ─── toEntity ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toEntity() — Account → AccountEntity")
    class ToEntity {

        @Test
        @DisplayName("lança NullPointerException quando domain é null")
        void deveLancarNpeParaDomainNull() {
            assertThatThrownBy(() -> AccountPersistenceMapper.toEntity(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Account must not be null");
        }

        @Test
        @DisplayName("mapeia Account.id para entity.externalId corretamente")
        void deveMapearIdParaExternalId() {
            AccountEntity result = AccountPersistenceMapper.toEntity(domainCompleto(AccountType.PERSONAL));
            assertThat(result.getExternalId()).isEqualTo(ID);
        }

        @Test
        @DisplayName("mapeia fullName para entity.fullName")
        void deveMapearFullName() {
            AccountEntity result = AccountPersistenceMapper.toEntity(domainCompleto(AccountType.PERSONAL));
            assertThat(result.getFullName()).isEqualTo(FULL_NAME);
        }

        @Test
        @DisplayName("mapeia CPF como dígitos brutos (value()), sem formatação")
        void deveMapearCpfComoDigitosBrutos() {
            AccountEntity result = AccountPersistenceMapper.toEntity(domainCompleto(AccountType.PERSONAL));
            // value() retorna os 11 dígitos sem máscara
            assertThat(result.getCpf()).isEqualTo(CPF);
            assertThat(result.getCpf()).doesNotContain(".");
            assertThat(result.getCpf()).doesNotContain("-");
        }

        @Test
        @DisplayName("mapeia email como string normalizada (value() — lowercase)")
        void deveMapearEmailNormalizado() {
            AccountEntity result = AccountPersistenceMapper.toEntity(domainCompleto(AccountType.PERSONAL));
            assertThat(result.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("mapeia accountType PERSONAL corretamente")
        void deveMapearAccountTypePersonal() {
            AccountEntity result = AccountPersistenceMapper.toEntity(domainCompleto(AccountType.PERSONAL));
            assertThat(result.getAccountType()).isEqualTo(AccountType.PERSONAL);
        }

        @Test
        @DisplayName("mapeia accountType BUSINESS corretamente")
        void deveMapearAccountTypeBusiness() {
            AccountEntity result = AccountPersistenceMapper.toEntity(domainCompleto(AccountType.BUSINESS));
            assertThat(result.getAccountType()).isEqualTo(AccountType.BUSINESS);
        }

        @Test
        @DisplayName("propaga createdAt do domínio para a entity (não nulo)")
        void devePropagapeCreatedAtNaoNulo() {
            AccountEntity result = AccountPersistenceMapper.toEntity(domainCompleto(AccountType.PERSONAL));
            // o builder usa o createdAt explícito informado
            assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
        }

        @Test
        @DisplayName("internalId permanece null — nunca definido pelo mapper")
        void internalIdDeveSerNulo() {
            AccountEntity result = AccountPersistenceMapper.toEntity(domainCompleto(AccountType.PERSONAL));
            assertThat(result.getInternalId()).isNull();
        }
    }

    // ─── round-trip ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Round-trip — domain → entity → domain deve ser equivalente")
    class RoundTrip {

        @Test
        @DisplayName("round-trip PERSONAL preserva todos os campos relevantes")
        void roundTripPersonal() {
            Account original = domainCompleto(AccountType.PERSONAL);

            AccountEntity entity = AccountPersistenceMapper.toEntity(original);
            // simula o que o banco devolveria (timestamps inalterados)
            entity.setUpdatedAt(original.getUpdatedAt());

            Account reconstituido = AccountPersistenceMapper.toDomain(entity);

            assertThat(reconstituido.getId()).isEqualTo(original.getId());
            assertThat(reconstituido.getFullName()).isEqualTo(original.getFullName());
            assertThat(reconstituido.getCpf()).isEqualTo(original.getCpf());
            assertThat(reconstituido.getEmail()).isEqualTo(original.getEmail());
            assertThat(reconstituido.getAccountType()).isEqualTo(original.getAccountType());
            assertThat(reconstituido.getCreatedAt()).isEqualTo(original.getCreatedAt());
        }

        @Test
        @DisplayName("round-trip BUSINESS preserva accountType")
        void roundTripBusiness() {
            Account original = domainCompleto(AccountType.BUSINESS);

            AccountEntity entity = AccountPersistenceMapper.toEntity(original);
            Account reconstituido = AccountPersistenceMapper.toDomain(entity);

            assertThat(reconstituido.getAccountType()).isEqualTo(AccountType.BUSINESS);
        }

        @Test
        @DisplayName("identidade por CPF é preservada no round-trip")
        void identidadePorCpfPreservada() {
            Account original = domainCompleto(AccountType.PERSONAL);

            AccountEntity entity = AccountPersistenceMapper.toEntity(original);
            Account reconstituido = AccountPersistenceMapper.toDomain(entity);

            // Account.equals() compara por CPF
            assertThat(reconstituido).isEqualTo(original);
            assertThat(reconstituido.hashCode()).isEqualTo(original.hashCode());
        }
    }
}

