package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.repository;

import com.github.sidneymiranda.fraudguard.accountservice.domain.Account;
import com.github.sidneymiranda.fraudguard.accountservice.domain.AccountType;
import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountEntity;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostgreSQLAccountRepository — Adapter de persistência JPA")
class PostgreSQLAccountRepositoryTest {

    // ─── fixtures ─────────────────────────────────────────────────────────────

    private static final UUID    EXT_ID     = UUID.randomUUID();
    private static final String  FULL_NAME  = "Sidney Miranda";
    private static final String  CPF        = "52998224725";
    private static final String  EMAIL      = "sidney@example.com";
    private static final Instant CREATED_AT = Instant.parse("2024-06-01T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2024-06-10T08:30:00Z");

    @Mock
    private PostgreSQLRepository jpaRepository;

    @InjectMocks
    private PostgreSQLAccountRepository repository;

    /** Domain Account para uso nos testes de escrita. */
    private Account accountDomain;

    /** Entity que o JPA "devolve" após save. */
    private AccountEntity savedEntity;

    @BeforeEach
    void setUp() {
        accountDomain = Account.builder()
                .id(EXT_ID)
                .fullName(FULL_NAME)
                .cpf(CPF)
                .email(EMAIL)
                .accountType(AccountType.PERSONAL)
                .createdAt(CREATED_AT)
                .build();

        savedEntity = buildEntity(FULL_NAME, EMAIL, AccountType.PERSONAL, CREATED_AT, UPDATED_AT);
    }

    /**
     * Fabrica uma {@link AccountEntity} com os parâmetros fornecidos.
     * Centraliza a criação de entidades de teste, evitando que cada caso
     * repita o bloco de setters um a um.
     */
    private AccountEntity buildEntity(String fullName, String email,
                                      AccountType type,
                                      Instant createdAt, Instant updatedAt) {
        AccountEntity e = new AccountEntity();
        e.setExternalId(EXT_ID);
        e.setFullName(fullName);
        e.setCpf(CPF);
        e.setEmail(email);
        e.setAccountType(type);
        e.setCreatedAt(createdAt);
        e.setUpdatedAt(updatedAt);
        return e;
    }

    // ─── save ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save() — persiste Account e retorna domínio mapeado")
    class Save {

        @Test
        @DisplayName("delega a chamada para jpaRepository.save com a entity mapeada")
        void deveDelegarParaJpaRepository() {
            when(jpaRepository.save(any(AccountEntity.class))).thenReturn(savedEntity);

            repository.save(accountDomain);

            verify(jpaRepository, times(1)).save(any(AccountEntity.class));
        }

        @Test
        @DisplayName("entity enviada ao JPA possui os campos corretos do domínio")
        void entityEnviadaPossuiCamposCorretos() {
            when(jpaRepository.save(any(AccountEntity.class))).thenReturn(savedEntity);
            ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);

            repository.save(accountDomain);

            verify(jpaRepository).save(captor.capture());
            AccountEntity captured = captor.getValue();

            assertThat(captured.getExternalId()).isEqualTo(EXT_ID);
            assertThat(captured.getFullName()).isEqualTo(FULL_NAME);
            assertThat(captured.getCpf()).isEqualTo(CPF);   // dígitos, sem máscara
            assertThat(captured.getEmail()).isEqualTo(EMAIL);
            assertThat(captured.getAccountType()).isEqualTo(AccountType.PERSONAL);
        }

        @Test
        @DisplayName("retorna Account de domínio mapeado a partir da entity devolvida pelo JPA")
        void deveRetornarDomainMapeadoDaEntitySalva() {
            when(jpaRepository.save(any(AccountEntity.class))).thenReturn(savedEntity);

            Account result = repository.save(accountDomain);

            assertThat(result.getId()).isEqualTo(EXT_ID);
            assertThat(result.getFullName()).isEqualTo(FULL_NAME);
            assertThat(result.getCpf().value()).isEqualTo(CPF);
            assertThat(result.getEmail().value()).isEqualTo(EMAIL);
            assertThat(result.getAccountType()).isEqualTo(AccountType.PERSONAL);
            assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
            assertThat(result.getUpdatedAt()).isEqualTo(UPDATED_AT);
        }

        @Test
        @DisplayName("propaga DataIntegrityViolationException quando JPA lança violação de constraint")
        void devePropagiarDataIntegrityViolation() {
            when(jpaRepository.save(any(AccountEntity.class)))
                    .thenThrow(new DataIntegrityViolationException("uq_account_cpf"));

            assertThatThrownBy(() -> repository.save(accountDomain))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uq_account_cpf");
        }

        @Test
        @DisplayName("save com AccountType BUSINESS mapeia o tipo corretamente")
        void deveSalvarContaTipoBusiness() {
            Account businessAccount = Account.builder()
                    .id(EXT_ID)
                    .fullName(FULL_NAME)
                    .cpf(CPF)
                    .email(EMAIL)
                    .accountType(AccountType.BUSINESS)
                    .createdAt(CREATED_AT)
                    .build();

            AccountEntity businessEntity = buildEntity(FULL_NAME, EMAIL, AccountType.BUSINESS, CREATED_AT, UPDATED_AT);
            when(jpaRepository.save(any(AccountEntity.class))).thenReturn(businessEntity);
            ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);

            Account result = repository.save(businessAccount);

            verify(jpaRepository).save(captor.capture());
            assertThat(captor.getValue().getAccountType()).isEqualTo(AccountType.BUSINESS);
            assertThat(result.getAccountType()).isEqualTo(AccountType.BUSINESS);
        }

        /**
         * Cenário: conta criada sem timestamps explícitos no domínio.
         * O banco, via {@code @PrePersist}, preenche createdAt e updatedAt.
         * O repositório deve devolver o que o banco retornou — nunca os valores
         * que foram enviados (que eram null antes do @PrePersist).
         */
        @Test
        @DisplayName("@PrePersist — timestamps gerados pelo banco são propagados ao domain retornado")
        void deveReflectirTimestampsAtribuidosPeloPrePersist() {
            Instant dbTimestamp = Instant.parse("2024-06-15T14:00:00Z");
            AccountEntity entityComTimestampsDoBanco =
                    buildEntity(FULL_NAME, EMAIL, AccountType.PERSONAL, dbTimestamp, dbTimestamp);

            when(jpaRepository.save(any(AccountEntity.class))).thenReturn(entityComTimestampsDoBanco);

            Account result = repository.save(accountDomain);

            // O repositório não pode inventar timestamps — deve refletir o que o banco devolveu
            assertThat(result.getCreatedAt()).isEqualTo(dbTimestamp);
            assertThat(result.getUpdatedAt()).isEqualTo(dbTimestamp);
        }

        /**
         * Cenário: o mapper nunca define {@code internalId} — a PK técnica é
         * responsabilidade exclusiva do banco via {@code @GeneratedValue(IDENTITY)}.
         * Se o repositório enviasse um internalId definido, o JPA poderia
         * tentar um UPDATE em vez de INSERT.
         */
        @Test
        @DisplayName("@GeneratedValue — entity enviada ao JPA não carrega internalId (PK técnica é do banco)")
        void entityEnviadaNaoPossuiInternalId() {
            when(jpaRepository.save(any(AccountEntity.class))).thenReturn(savedEntity);
            ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);

            repository.save(accountDomain);

            verify(jpaRepository).save(captor.capture());
            assertThat(captor.getValue().getInternalId())
                    .as("internalId deve ser null ao enviar para o JPA — geração é responsabilidade do banco")
                    .isNull();
        }
    }

    // ─── findById ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById() — busca por externalId (UUID de negócio)")
    class FindById {

        @Test
        @DisplayName("delega para jpaRepository.findByExternalId com o UUID correto")
        void deveDelegarParaFindByExternalId() {
            when(jpaRepository.findByExternalId(EXT_ID)).thenReturn(Optional.of(savedEntity));

            repository.findById(EXT_ID);

            verify(jpaRepository).findByExternalId(EXT_ID);
        }

        @Test
        @DisplayName("retorna Optional com Account mapeado quando entity é encontrada")
        void deveRetornarAccountQuandoEncontrado() {
            when(jpaRepository.findByExternalId(EXT_ID)).thenReturn(Optional.of(savedEntity));

            Optional<Account> result = repository.findById(EXT_ID);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(EXT_ID);
            assertThat(result.get().getCpf().value()).isEqualTo(CPF);
            assertThat(result.get().getEmail().value()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("retorna Optional.empty() quando entity não é encontrada")
        void deveRetornarEmptyQuandoNaoEncontrado() {
            when(jpaRepository.findByExternalId(EXT_ID)).thenReturn(Optional.empty());

            Optional<Account> result = repository.findById(EXT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Account retornado preserva todos os campos da entity — incluindo updatedAt")
        void devePreservarCamposDaEntity() {
            when(jpaRepository.findByExternalId(EXT_ID)).thenReturn(Optional.of(savedEntity));

            Account found = repository.findById(EXT_ID).orElseThrow();

            assertThat(found.getFullName()).isEqualTo(FULL_NAME);
            assertThat(found.getAccountType()).isEqualTo(AccountType.PERSONAL);
            assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
            assertThat(found.getUpdatedAt()).isEqualTo(UPDATED_AT);
        }

        @Test
        @DisplayName("busca por UUID diferente retorna empty — não confunde IDs")
        void buscarPorIdDiferenteRetornaEmpty() {
            UUID outroId = UUID.randomUUID();
            when(jpaRepository.findByExternalId(outroId)).thenReturn(Optional.empty());

            Optional<Account> result = repository.findById(outroId);

            assertThat(result).isEmpty();
            verify(jpaRepository).findByExternalId(outroId);
            verify(jpaRepository, never()).findByExternalId(EXT_ID);
        }

        /**
         * Cenário: conta recuperada do banco após uma atualização de perfil.
         * O JPA terá invocado {@code @PreUpdate} internamente, avançando {@code updatedAt}.
         * O repositório deve refletir fielmente esses timestamps — sem recalcular,
         * sem ignorar o que o banco devolveu.
         */
        @Test
        @DisplayName("@PreUpdate — domain reconstituto reflete updatedAt posterior ao createdAt após atualização de perfil")
        void deveReflectirUpdatedAtAvancadoPorPreUpdate() {
            // Simula o estado do banco após um updateProfile: nome e email trocados,
            // updatedAt avançado pelo @PreUpdate — createdAt permanece o original
            AccountEntity entityAposUpdate =
                    buildEntity("Nome Atualizado", "atualizado@example.com", AccountType.PERSONAL, CREATED_AT, UPDATED_AT);

            when(jpaRepository.findByExternalId(EXT_ID)).thenReturn(Optional.of(entityAposUpdate));

            Account found = repository.findById(EXT_ID).orElseThrow();

            // updatedAt deve ser posterior ao createdAt — evidência de que o @PreUpdate foi aplicado
            assertThat(found.getUpdatedAt())
                    .as("updatedAt deve ser posterior a createdAt após atualização de perfil")
                    .isAfter(found.getCreatedAt());
            assertThat(found.getUpdatedAt()).isEqualTo(UPDATED_AT);
            assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
            // dados do perfil atualizado também chegam corretamente ao domain
            assertThat(found.getFullName()).isEqualTo("Nome Atualizado");
            assertThat(found.getEmail().value()).isEqualTo("atualizado@example.com");
        }
    }

    // ─── findByCpf ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByCpf() — busca por CPF")
    class FindByCpf {

        @Test
        @DisplayName("delega para jpaRepository.findByCpf com os dígitos corretos")
        void deveDelegarParaFindByCpf() {
            when(jpaRepository.findByCpf(CPF)).thenReturn(Optional.of(savedEntity));

            repository.findByCpf(CPF);

            verify(jpaRepository).findByCpf(CPF);
        }

        @Test
        @DisplayName("retorna Optional com Account mapeado quando CPF existe")
        void deveRetornarAccountQuandoCpfExiste() {
            when(jpaRepository.findByCpf(CPF)).thenReturn(Optional.of(savedEntity));

            Optional<Account> result = repository.findByCpf(CPF);

            assertThat(result).isPresent();
            assertThat(result.get().getCpf().value()).isEqualTo(CPF);
            assertThat(result.get().getId()).isEqualTo(EXT_ID);
        }

        @Test
        @DisplayName("retorna Optional.empty() quando CPF não está cadastrado")
        void deveRetornarEmptyQuandoCpfNaoExiste() {
            when(jpaRepository.findByCpf(CPF)).thenReturn(Optional.empty());

            Optional<Account> result = repository.findByCpf(CPF);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("CPF não passado para outros métodos — delegação isolada")
        void delegacaoIsolada() {
            when(jpaRepository.findByCpf(CPF)).thenReturn(Optional.of(savedEntity));

            repository.findByCpf(CPF);

            verify(jpaRepository).findByCpf(CPF);
            verify(jpaRepository, never()).findByExternalId(any());
            verify(jpaRepository, never()).existsByCpf(any());
        }
    }

    // ─── existsByCpf ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("existsByCpf() — verifica existência por CPF")
    class ExistsByCpf {

        @Test
        @DisplayName("retorna true quando CPF já está cadastrado")
        void deveRetornarTrueQuandoExiste() {
            when(jpaRepository.existsByCpf(CPF)).thenReturn(true);

            assertThat(repository.existsByCpf(CPF)).isTrue();
        }

        @Test
        @DisplayName("retorna false quando CPF não está cadastrado")
        void deveRetornarFalseQuandoNaoExiste() {
            when(jpaRepository.existsByCpf(CPF)).thenReturn(false);

            assertThat(repository.existsByCpf(CPF)).isFalse();
        }

        @Test
        @DisplayName("delega exatamente o CPF recebido ao JPA — sem transformação")
        void deveDelegarCpfSemTransformacao() {
            when(jpaRepository.existsByCpf(CPF)).thenReturn(true);

            repository.existsByCpf(CPF);

            verify(jpaRepository).existsByCpf(CPF);
            verifyNoMoreInteractions(jpaRepository);
        }
    }

    // ─── existsByEmail ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("existsByEmail() — verifica existência por e-mail")
    class ExistsByEmail {

        @Test
        @DisplayName("retorna true quando e-mail já está cadastrado")
        void deveRetornarTrueQuandoExiste() {
            when(jpaRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThat(repository.existsByEmail(EMAIL)).isTrue();
        }

        @Test
        @DisplayName("retorna false quando e-mail não está cadastrado")
        void deveRetornarFalseQuandoNaoExiste() {
            when(jpaRepository.existsByEmail(EMAIL)).thenReturn(false);

            assertThat(repository.existsByEmail(EMAIL)).isFalse();
        }

        @Test
        @DisplayName("delega exatamente o e-mail recebido ao JPA — sem transformação")
        void deveDelegarEmailSemTransformacao() {
            when(jpaRepository.existsByEmail(EMAIL)).thenReturn(false);

            repository.existsByEmail(EMAIL);

            verify(jpaRepository).existsByEmail(EMAIL);
            verifyNoMoreInteractions(jpaRepository);
        }

        @Test
        @DisplayName("verifica e-mail em caixa baixa — conforme armazenado pelo domínio")
        void deveVerificarEmailEmCaixaBaixa() {
            String emailMinusculo = "sidney@example.com";
            when(jpaRepository.existsByEmail(emailMinusculo)).thenReturn(true);

            assertThat(repository.existsByEmail(emailMinusculo)).isTrue();
            verify(jpaRepository).existsByEmail(emailMinusculo);
        }
    }

    // ─── comportamento transversal ────────────────────────────────────────────

    @Nested
    @DisplayName("Contrato transversal — nenhum método acessa o JPA além do esperado")
    class ContratoTransversal {

        @Test
        @DisplayName("save() não invoca findByCpf, findByExternalId, existsByCpf nem existsByEmail")
        void saveNaoInvocaMetodosDeConsulta() {
            when(jpaRepository.save(any())).thenReturn(savedEntity);

            repository.save(accountDomain);

            verify(jpaRepository, never()).findByCpf(any());
            verify(jpaRepository, never()).findByExternalId(any());
            verify(jpaRepository, never()).existsByCpf(any());
            verify(jpaRepository, never()).existsByEmail(any());
        }

        @Test
        @DisplayName("existsByCpf() não invoca save nem métodos de busca por entidade")
        void existsByCpfNaoInvocaOutrosMetodos() {
            when(jpaRepository.existsByCpf(CPF)).thenReturn(false);

            repository.existsByCpf(CPF);

            verify(jpaRepository, never()).save(any());
            verify(jpaRepository, never()).findByCpf(any());
            verify(jpaRepository, never()).findByExternalId(any());
        }

        @Test
        @DisplayName("existsByEmail() não invoca save nem métodos de busca por entidade")
        void existsByEmailNaoInvocaOutrosMetodos() {
            when(jpaRepository.existsByEmail(EMAIL)).thenReturn(false);

            repository.existsByEmail(EMAIL);

            verify(jpaRepository, never()).save(any());
            verify(jpaRepository, never()).findByCpf(any());
            verify(jpaRepository, never()).findByExternalId(any());
        }
    }
}


