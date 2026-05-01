package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.repository;

import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountOutboxEventEntity;
import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountOutboxEventEntity.OutboxEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostgreSQLAccountOutboxRepository — Adapter de persistência de outbox")
class PostgreSQLAccountOutboxRepositoryTest {

    // ─── fixtures ─────────────────────────────────────────────────────────────

    private static final UUID   USER_ID = UUID.randomUUID();
    private static final String TOPIC   = "account.created";
    private static final String PAYLOAD = "{\"userId\":\"" + USER_ID + "\",\"accountType\":\"PERSONAL\"}";

    @Mock
    private PostgreSQLOutboxRepository jpaRepository;

    @InjectMocks
    private PostgreSQLAccountOutboxRepository repository;

    /** Entidade que simula o retorno do JPA após o INSERT. */
    private AccountOutboxEventEntity savedEntity;

    @BeforeEach
    void setUp() {
        savedEntity = buildEntity(OutboxEventStatus.PENDING, null);
    }

    /**
     * Fabrica um {@link AccountOutboxEventEntity} com os parâmetros fornecidos.
     * {@code createdAt} é gerenciado exclusivamente por {@code @PrePersist} — não possui setter:
     * em testes unitários permanece {@code null}, comportamento verificado em integração.
     */
    private AccountOutboxEventEntity buildEntity(OutboxEventStatus status, Instant sentAt) {
        AccountOutboxEventEntity entity = new AccountOutboxEventEntity();
        entity.setUserId(USER_ID);
        entity.setTopic(TOPIC);
        entity.setPayload(PAYLOAD);
        entity.setStatus(status);
        entity.setSentAt(sentAt);
        return entity;
    }

    // ─── save ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save() — persiste AccountOutboxEventEntity e retorna a entidade salva")
    class Save {

        @Test
        @DisplayName("delega a chamada para jpaRepository.save com a mesma entidade recebida")
        void deveDelegarParaJpaRepository() {
            when(jpaRepository.save(any(AccountOutboxEventEntity.class))).thenReturn(savedEntity);

            repository.save(savedEntity);

            verify(jpaRepository, times(1)).save(any(AccountOutboxEventEntity.class));
        }

        @Test
        @DisplayName("entity enviada ao JPA preserva userId, topic e payload sem transformação")
        void entityEnviadaPreservaCamposSemTransformacao() {
            when(jpaRepository.save(any(AccountOutboxEventEntity.class))).thenReturn(savedEntity);
            ArgumentCaptor<AccountOutboxEventEntity> captor =
                    ArgumentCaptor.forClass(AccountOutboxEventEntity.class);

            repository.save(savedEntity);

            verify(jpaRepository).save(captor.capture());
            AccountOutboxEventEntity captured = captor.getValue();

            assertThat(captured.getUserId()).isEqualTo(USER_ID);
            assertThat(captured.getTopic()).isEqualTo(TOPIC);
            assertThat(captured.getPayload()).isEqualTo(PAYLOAD);
        }

        @Test
        @DisplayName("retorna exatamente a entidade devolvida pelo jpaRepository")
        void deveRetornarEntidadeDevolvida() {
            when(jpaRepository.save(any(AccountOutboxEventEntity.class))).thenReturn(savedEntity);

            AccountOutboxEventEntity result = repository.save(savedEntity);

            assertThat(result).isSameAs(savedEntity);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getTopic()).isEqualTo(TOPIC);
            assertThat(result.getPayload()).isEqualTo(PAYLOAD);
            assertThat(result.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(result.getSentAt()).isNull();
        }

        @Test
        @DisplayName("lança NullPointerException quando event é nulo — fail-fast antes de delegar ao JPA")
        void deveLancarNullPointerExceptionParaEventoNulo() {
            assertThatThrownBy(() -> repository.save(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("AccountOutboxEventEntity must not be null");

            verifyNoInteractions(jpaRepository);
        }

        @Test
        @DisplayName("propaga exceção lançada pelo jpaRepository sem encapsulamento")
        void devePropagarExcecaoDoJpaRepository() {
            when(jpaRepository.save(any(AccountOutboxEventEntity.class)))
                    .thenThrow(new DataIntegrityViolationException("fk_outbox_user_id"));

            assertThatThrownBy(() -> repository.save(savedEntity))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("fk_outbox_user_id");
        }

        @Test
        @DisplayName("entidade com status SENT é persistida sem alteração de status pelo adapter")
        void devePersistirEntityComStatusSent() {
            Instant sentAt = Instant.parse("2024-06-01T13:00:00Z");
            AccountOutboxEventEntity sentEntity = buildEntity(OutboxEventStatus.SENT, sentAt);
            when(jpaRepository.save(any(AccountOutboxEventEntity.class))).thenReturn(sentEntity);
            ArgumentCaptor<AccountOutboxEventEntity> captor =
                    ArgumentCaptor.forClass(AccountOutboxEventEntity.class);

            AccountOutboxEventEntity result = repository.save(sentEntity);

            verify(jpaRepository).save(captor.capture());
            // O adapter não altera status — reponsabilidade do chamador
            assertThat(captor.getValue().getStatus()).isEqualTo(OutboxEventStatus.SENT);
            assertThat(result.getSentAt()).isEqualTo(sentAt);
        }

        @Test
        @DisplayName("@PrePersist — createdAt não é gerenciado pelo adapter (responsabilidade do ciclo JPA)")
        void createdAtNaoEGeridoPeloAdapter() {
            // O adapter não possui lógica de timestamp — @PrePersist da entidade é responsável.
            // Aqui validamos que a entidade retornada é fielmente devolvida, sem modificação.
            when(jpaRepository.save(any(AccountOutboxEventEntity.class))).thenReturn(savedEntity);

            AccountOutboxEventEntity result = repository.save(savedEntity);

            // O adapter devolve exatamente o que o jpaRepository retornou — sem reescrever campos
            assertThat(result).isSameAs(savedEntity);
        }
    }

    // ─── contrato transversal ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Contrato transversal — save() acessa apenas jpaRepository.save()")
    class ContratoTransversal {

        @Test
        @DisplayName("save() não invoca nenhum outro método do jpaRepository além de save")
        void saveNaoInvocaMetodosAdicionais() {
            when(jpaRepository.save(any(AccountOutboxEventEntity.class))).thenReturn(savedEntity);

            repository.save(savedEntity);

            verify(jpaRepository, times(1)).save(any(AccountOutboxEventEntity.class));
            verifyNoMoreInteractions(jpaRepository);
        }

        @Test
        @DisplayName("save() com nulo não chega a invocar o jpaRepository — validação antecipada")
        void saveComNuloNaoAtingeJpaRepository() {
            assertThatThrownBy(() -> repository.save(null))
                    .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(jpaRepository);
        }
    }
}




