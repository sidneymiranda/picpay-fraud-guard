package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity;

import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountOutboxEventEntity.OutboxEventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do callback JPA {@code @PrePersist} e do getter {@code getId()}.
 *
 * <p>{@code prePersist()} é {@code private} e só dispara via {@code EntityManager} em runtime —
 * nunca através dos mocks usados nos testes do repositório. Por isso, a única forma de
 * cobrir seus branches em testes unitários é invocá-lo via reflection, seguindo o mesmo
 * padrão adotado em {@code AccountEntityTest}.
 *
 * <p>{@code getId()} expõe a PK técnica gerada pelo banco ({@code @GeneratedValue(IDENTITY)}).
 * Em cenários de teste, o valor é injetado via reflection para simular o retorno pós-INSERT.
 */
@DisplayName("AccountOutboxEventEntity — @PrePersist e getId()")
class AccountOutboxEventEntityTest {

    private static final Instant FIXED_CREATED = Instant.parse("2024-06-01T12:00:00Z");

    private void invokePrePersist(AccountOutboxEventEntity entity) throws Exception {
        Method m = AccountOutboxEventEntity.class.getDeclaredMethod("prePersist");
        m.setAccessible(true);
        m.invoke(entity);
    }

    private AccountOutboxEventEntity buildEntity(Instant createdAt, OutboxEventStatus status) {
        AccountOutboxEventEntity entity = new AccountOutboxEventEntity();
        entity.setUserId(UUID.randomUUID());
        entity.setTopic("account.created");
        entity.setPayload("{}");
        entity.setStatus(status);
        // createdAt não tem setter — injetado via reflection para testar branch "não nulo"
        if (createdAt != null) {
            setCreatedAtViaReflection(entity, createdAt);
        }
        return entity;
    }

    private void setCreatedAtViaReflection(AccountOutboxEventEntity entity, Instant value) {
        try {
            Field field = AccountOutboxEventEntity.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, value);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao injetar createdAt via reflection", e);
        }
    }

    // ─── @PrePersist ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("@PrePersist — inicialização defensiva de createdAt e status")
    class PrePersist {

        @Test
        @DisplayName("preenche createdAt quando null — branch: createdAt == null → true")
        void deveDefinirCreatedAtQuandoNull() throws Exception {
            AccountOutboxEventEntity entity = buildEntity(null, OutboxEventStatus.PENDING);
            Instant antes = Instant.now();

            invokePrePersist(entity);

            assertThat(entity.getCreatedAt())
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("NÃO sobrescreve createdAt já definido — branch: createdAt == null → false")
        void naoDeveSubstituirCreatedAtJaDefinido() throws Exception {
            AccountOutboxEventEntity entity = buildEntity(FIXED_CREATED, OutboxEventStatus.PENDING);

            invokePrePersist(entity);

            assertThat(entity.getCreatedAt()).isEqualTo(FIXED_CREATED);
        }

        @Test
        @DisplayName("preenche status com PENDING quando null — branch: status == null → true")
        void deveDefinirStatusPendingQuandoNull() throws Exception {
            AccountOutboxEventEntity entity = buildEntity(FIXED_CREATED, null);

            invokePrePersist(entity);

            assertThat(entity.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        }

        @Test
        @DisplayName("NÃO sobrescreve status já definido — branch: status == null → false")
        void naoDeveSubstituirStatusJaDefinido() throws Exception {
            AccountOutboxEventEntity entity = buildEntity(FIXED_CREATED, OutboxEventStatus.SENT);

            invokePrePersist(entity);

            assertThat(entity.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        }

        @Test
        @DisplayName("preenche createdAt e status quando ambos são null")
        void devePreencherAmbosQuandoNull() throws Exception {
            AccountOutboxEventEntity entity = buildEntity(null, null);
            Instant antes = Instant.now();

            invokePrePersist(entity);

            assertThat(entity.getCreatedAt())
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(Instant.now());
            assertThat(entity.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        }

        @Test
        @DisplayName("não altera nenhum campo quando createdAt e status já estão definidos")
        void naoDeveAlterarNadaQuandoAmbosDefinidos() throws Exception {
            AccountOutboxEventEntity entity = buildEntity(FIXED_CREATED, OutboxEventStatus.SENT);

            invokePrePersist(entity);

            assertThat(entity.getCreatedAt()).isEqualTo(FIXED_CREATED);
            assertThat(entity.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        }
    }

    // ─── getId ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getId() — PK técnica gerada pelo banco")
    class GetId {

        @Test
        @DisplayName("retorna null para entidade nova (pré-INSERT) — id gerado pelo banco via @GeneratedValue")
        void deveRetornarNullParaEntidadeNova() {
            AccountOutboxEventEntity entity = new AccountOutboxEventEntity();

            assertThat(entity.getId()).isNull();
        }

        @Test
        @DisplayName("retorna o id injetado — simula estado pós-INSERT do banco")
        void deveRetornarIdAposInsert() throws Exception {
            AccountOutboxEventEntity entity = new AccountOutboxEventEntity();
            Field idField = AccountOutboxEventEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, 42L);

            assertThat(entity.getId()).isEqualTo(42L);
        }
    }
}

