package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity;

import com.github.sidneymiranda.fraudguard.accountservice.domain.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Testes unitários dos callbacks JPA {@code @PrePersist} e {@code @PreUpdate}.
 *
 * <p>Esses métodos são {@code private} e só disparam via {@code EntityManager} — nunca
 * através do mock de {@code PostgreSQLRepository} usado no repositório. Por isso, a única
 * forma de cobri-los em testes unitários é invocá-los via reflection.
 * Getters e setters da entidade não têm testes aqui: seu comportamento é validado
 * transversalmente em {@code AccountPersistenceMapperTest} e
 * {@code PostgreSQLAccountRepositoryTest}.
 */
@DisplayName("AccountEntity — callbacks JPA @PrePersist / @PreUpdate")
class AccountEntityTest {

    private static final Instant FIXED_CREATED = Instant.parse("2024-01-01T10:00:00Z");
    private static final Instant FIXED_UPDATED = Instant.parse("2024-01-02T08:00:00Z");

    private void invokePrePersist(AccountEntity entity) throws Exception {
        Method m = AccountEntity.class.getDeclaredMethod("prePersist");
        m.setAccessible(true);
        m.invoke(entity);
    }

    private void invokePreUpdate(AccountEntity entity) throws Exception {
        Method m = AccountEntity.class.getDeclaredMethod("preUpdate");
        m.setAccessible(true);
        m.invoke(entity);
    }

    private AccountEntity entityWithTimestamps(Instant createdAt, Instant updatedAt) {
        AccountEntity e = new AccountEntity();
        e.setAccountType(AccountType.PERSONAL);
        e.setCreatedAt(createdAt);
        e.setUpdatedAt(updatedAt);
        return e;
    }

    // ─── @PrePersist ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("@PrePersist — inicialização defensiva de timestamps")
    class PrePersist {

        @Test
        @DisplayName("preenche createdAt quando null — branch: createdAt == null → true")
        void deveDefinirCreatedAtQuandoNull() throws Exception {
            AccountEntity entity = entityWithTimestamps(null, null);
            Instant antes = Instant.now();

            invokePrePersist(entity);

            assertThat(entity.getCreatedAt())
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("preenche updatedAt quando null — branch: updatedAt == null → true")
        void deveDefinirUpdatedAtQuandoNull() throws Exception {
            AccountEntity entity = entityWithTimestamps(null, null);
            Instant antes = Instant.now();

            invokePrePersist(entity);

            assertThat(entity.getUpdatedAt())
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("NÃO sobrescreve createdAt já definido — branch: createdAt == null → false")
        void naoDeveSubstituirCreatedAtJaDefinido() throws Exception {
            AccountEntity entity = entityWithTimestamps(FIXED_CREATED, null);

            invokePrePersist(entity);

            assertThat(entity.getCreatedAt()).isEqualTo(FIXED_CREATED);
        }

        @Test
        @DisplayName("NÃO sobrescreve updatedAt já definido — branch: updatedAt == null → false")
        void naoDeveSubstituirUpdatedAtJaDefinido() throws Exception {
            AccountEntity entity = entityWithTimestamps(null, FIXED_UPDATED);

            invokePrePersist(entity);

            assertThat(entity.getUpdatedAt()).isEqualTo(FIXED_UPDATED);
        }

        @Test
        @DisplayName("createdAt e updatedAt ficam no mesmo segundo quando ambos são null")
        void timestampsDevemEstarNoMesmoSegundoQuandoAmbosNull() throws Exception {
            AccountEntity entity = entityWithTimestamps(null, null);

            invokePrePersist(entity);

            assertThat(entity.getCreatedAt())
                    .isCloseTo(entity.getUpdatedAt(), within(1, SECONDS));
        }
    }

    // ─── @PreUpdate ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("@PreUpdate — avanço incondicional de updatedAt")
    class PreUpdate {

        @Test
        @DisplayName("substitui updatedAt pelo instante atual — sempre, independente do valor anterior")
        void deveAtualizarUpdatedAtParaAgora() throws Exception {
            AccountEntity entity = entityWithTimestamps(FIXED_CREATED, FIXED_UPDATED);
            Instant antes = Instant.now();

            invokePreUpdate(entity);

            assertThat(entity.getUpdatedAt())
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("updatedAt após @PreUpdate é posterior ao FIXED_UPDATED original")
        void updatedAtDeveSerPosteriorAoValorAnterior() throws Exception {
            AccountEntity entity = entityWithTimestamps(FIXED_CREATED, FIXED_UPDATED);

            invokePreUpdate(entity);

            assertThat(entity.getUpdatedAt()).isAfter(FIXED_UPDATED);
        }

        @Test
        @DisplayName("@PreUpdate não altera createdAt")
        void naoDeveAlterarCreatedAt() throws Exception {
            AccountEntity entity = entityWithTimestamps(FIXED_CREATED, FIXED_UPDATED);

            invokePreUpdate(entity);

            assertThat(entity.getCreatedAt()).isEqualTo(FIXED_CREATED);
        }
    }
}

