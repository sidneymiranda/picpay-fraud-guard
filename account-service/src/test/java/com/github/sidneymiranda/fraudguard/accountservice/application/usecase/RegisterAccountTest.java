package com.github.sidneymiranda.fraudguard.accountservice.application.usecase;

import com.github.sidneymiranda.fraudguard.accountservice.application.dto.AccountCreatedResponse;
import com.github.sidneymiranda.fraudguard.accountservice.application.dto.RegisterAccountRequest;
import com.github.sidneymiranda.fraudguard.accountservice.application.exception.AccountRegistrationException;
import com.github.sidneymiranda.fraudguard.accountservice.application.gateway.EventPublisher;
import com.github.sidneymiranda.fraudguard.accountservice.application.gateway.IdentityProvider;
import com.github.sidneymiranda.fraudguard.accountservice.domain.Account;
import com.github.sidneymiranda.fraudguard.accountservice.domain.AccountType;
import com.github.sidneymiranda.fraudguard.accountservice.domain.event.AccountCreatedEvent;
import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.CpfAlreadyExistsException;
import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.EmailAlreadyExistsException;
import com.github.sidneymiranda.fraudguard.accountservice.domain.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("RegisterAccount — Caso de uso de registro de conta (RF-01, RF-03)")
class RegisterAccountTest {

    // ─── fixtures ─────────────────────────────────────────────────────────────

    static final String PROVIDER_ID  = UUID.randomUUID().toString();
    static final String FULL_NAME    = "Sidney Miranda";
    static final String CPF          = "52998224725";
    static final String EMAIL        = "sidney@example.com";
    static final String PASSWORD     = "s3cr3t";

    @Mock AccountRepository          repository;
    @Mock IdentityProvider           identityProvider;
    @Mock EventPublisher             eventPublisher;

    @InjectMocks
    RegisterAccount registerAccount;

    /**
     * Configura o estado padrão do happy path.
     * Lenient para que testes que falham antes de certas etapas não causem UnnecessaryStubbing.
     */
    @BeforeEach
    void setUp() {
        when(repository.existsByCpf(CPF)).thenReturn(false);
        when(repository.existsByEmail(EMAIL)).thenReturn(false);
        when(identityProvider.createUser(EMAIL, PASSWORD, FULL_NAME)).thenReturn(PROVIDER_ID);
        // repository.save devolve a mesma Account recebida
        when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private RegisterAccountRequest requestValido() {
        return new RegisterAccountRequest(FULL_NAME, CPF, EMAIL, PASSWORD, AccountType.PERSONAL);
    }

    // ─── caminho feliz ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register() — caminho feliz")
    class CaminhoFeliz {

        @Test
        @DisplayName("retorna AccountCreatedResponse com o ID gerado pelo provedor de identidade")
        void deveRetornarResponseComIdDoProvedor() {
            AccountCreatedResponse response = registerAccount.register(requestValido());

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(UUID.fromString(PROVIDER_ID));
        }

        @Test
        @DisplayName("persiste a Account com os dados do request")
        void devePersistirAccountComDadosDoRequest() {
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

            registerAccount.register(requestValido());

            verify(repository).save(captor.capture());
            Account saved = captor.getValue();
            assertThat(saved.getId()).isEqualTo(UUID.fromString(PROVIDER_ID));
            assertThat(saved.getCpf().value()).isEqualTo(CPF);
            assertThat(saved.getEmail().value()).isEqualTo(EMAIL);
            assertThat(saved.getFullName()).isEqualTo(FULL_NAME);
            assertThat(saved.getAccountType()).isEqualTo(AccountType.PERSONAL);
        }

        @Test
        @DisplayName("publica AccountCreatedEvent após persistência bem-sucedida")
        void devePublicarEventoAposPersistencia() {
            ArgumentCaptor<AccountCreatedEvent> captor = ArgumentCaptor.forClass(AccountCreatedEvent.class);

            registerAccount.register(requestValido());

            verify(eventPublisher).publish(captor.capture());
            AccountCreatedEvent event = captor.getValue();
            assertThat(event.userId()).isEqualTo(UUID.fromString(PROVIDER_ID));
            assertThat(event.email()).isEqualTo(EMAIL);
            assertThat(event.accountType()).isEqualTo(AccountType.PERSONAL);
        }

        @Test
        @DisplayName("accountType null no request resolve para PERSONAL (default)")
        void deveUsarPersonalComoDefaultParaAccountTypeNulo() {
            RegisterAccountRequest semTipo = new RegisterAccountRequest(FULL_NAME, CPF, EMAIL, PASSWORD, null);
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

            registerAccount.register(semTipo);

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAccountType()).isEqualTo(AccountType.PERSONAL);
        }

        @Test
        @DisplayName("accountType BUSINESS é respeitado quando informado")
        void deveUsarAccountTypeBusinessQuandoInformado() {
            RegisterAccountRequest comBusiness = new RegisterAccountRequest(FULL_NAME, CPF, EMAIL, PASSWORD, AccountType.BUSINESS);
            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

            registerAccount.register(comBusiness);

            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getAccountType()).isEqualTo(AccountType.BUSINESS);
        }

        @Test
        @DisplayName("verifica unicidade de CPF e email antes de chamar o provedor de identidade")
        void deveVerificarUnicidadeAntesDeIrAoProvedor() {
            registerAccount.register(requestValido());

            // a ordem importa: unicidade primeiro, provedor de identidade depois
            var inOrder = inOrder(repository, identityProvider);
            inOrder.verify(repository).existsByCpf(CPF);
            inOrder.verify(repository).existsByEmail(EMAIL);
            inOrder.verify(identityProvider).createUser(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("persiste a Account antes de publicar o evento")
        void devePersistirAntesDePublicarEvento() {
            registerAccount.register(requestValido());

            var inOrder = inOrder(repository, eventPublisher);
            inOrder.verify(repository).save(any());
            inOrder.verify(eventPublisher).publish(any());
        }
    }

    // ─── validações de negócio (RF-01) ────────────────────────────────────────

    @Nested
    @DisplayName("register() — violações de unicidade (RF-01)")
    class ValidacoesNegocio {

        @Test
        @DisplayName("lança CpfAlreadyExistsException quando CPF já cadastrado")
        void deveLancarExcecaoParaCpfDuplicado() {
            when(repository.existsByCpf(CPF)).thenReturn(true);

            assertThatThrownBy(() -> registerAccount.register(requestValido()))
                    .isInstanceOf(CpfAlreadyExistsException.class)
                    .hasMessageContaining(CPF);
        }

        @Test
        @DisplayName("não chama o provedor de identidade quando CPF já está cadastrado")
        void naoDeveChamarProvedorParaCpfDuplicado() {
            when(repository.existsByCpf(CPF)).thenReturn(true);

            assertThatThrownBy(() -> registerAccount.register(requestValido()))
                    .isInstanceOf(CpfAlreadyExistsException.class);

            verifyNoInteractions(identityProvider);
        }

        @Test
        @DisplayName("lança EmailAlreadyExistsException quando e-mail já cadastrado")
        void deveLancarExcecaoParaEmailDuplicado() {
            when(repository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> registerAccount.register(requestValido()))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining(EMAIL);
        }

        @Test
        @DisplayName("não chama o provedor de identidade quando e-mail já está cadastrado")
        void naoDeveChamarProvedorParaEmailDuplicado() {
            when(repository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> registerAccount.register(requestValido()))
                    .isInstanceOf(EmailAlreadyExistsException.class);

            verifyNoInteractions(identityProvider);
        }

        @Test
        @DisplayName("CPF é verificado antes do e-mail — menor custo primeiro")
        void deveVerificarCpfAntesDoEmail() {
            when(repository.existsByCpf(CPF)).thenReturn(true);

            assertThatThrownBy(() -> registerAccount.register(requestValido()))
                    .isInstanceOf(CpfAlreadyExistsException.class);

            // se CPF falhou, email nem é verificado
            verify(repository, never()).existsByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("register() — SAGA: compensação no provedor de identidade")
    class SagaCompensacao {

        @Test
        @DisplayName("chama deleteUser no provedor quando repository.save lança exceção")
        void deveCompensarNoProvedorQuandoSaveFalha() {
            when(repository.save(any())).thenThrow(new RuntimeException("DB offline"));

            assertThatThrownBy(() -> registerAccount.register(requestValido()))
                    .isInstanceOf(AccountRegistrationException.class);

            verify(identityProvider).deleteUser(PROVIDER_ID);
        }

        @Test
        @DisplayName("chama deleteUser no provedor quando eventPublisher.publish lança exceção")
        void deveCompensarNoProvedorQuandoPublicacaoFalha() {
            doThrow(new RuntimeException("falha na publicação do evento")).when(eventPublisher).publish(any());

            assertThatThrownBy(() -> registerAccount.register(requestValido()))
                    .isInstanceOf(AccountRegistrationException.class);

            verify(identityProvider).deleteUser(PROVIDER_ID);
        }

        @Test
        @DisplayName("AccountRegistrationException encapsula a causa raiz original")
        void excecaoDeveEncapsularCausaOriginal() {
            RuntimeException causaRaiz = new RuntimeException("DB timeout");
            when(repository.save(any())).thenThrow(causaRaiz);

            assertThatThrownBy(() -> registerAccount.register(requestValido()))
                    .isInstanceOf(AccountRegistrationException.class)
                    .hasCause(causaRaiz);
        }

        @Test
        @DisplayName("eventPublisher nunca é chamado quando repository.save falha")
        void naoDevePublicarEventoQuandoSaveFalha() {
            when(repository.save(any())).thenThrow(new RuntimeException("DB offline"));

            assertThatThrownBy(() -> registerAccount.register(requestValido()))
                    .isInstanceOf(AccountRegistrationException.class);

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("deleteUser recebe exatamente o ID retornado pelo provedor na criação")
        void deleteUserDeveReceberOIdCorreto() {
            String uuidEspecifico = UUID.randomUUID().toString();
            when(identityProvider.createUser(EMAIL, PASSWORD, FULL_NAME)).thenReturn(uuidEspecifico);
            when(repository.save(any())).thenThrow(new RuntimeException("falha"));

            assertThatThrownBy(() -> registerAccount.register(requestValido()))
                    .isInstanceOf(AccountRegistrationException.class);

            verify(identityProvider).deleteUser(eq(uuidEspecifico));
        }

        @Test
        @DisplayName("deleteUser não é chamado quando a falha ocorre antes do provedor (CPF duplicado)")
        void naoDeveCompensarNoProvedorParaFalhaAnteriorAEle() {
            when(repository.existsByCpf(CPF)).thenReturn(true);

            assertThatThrownBy(() -> registerAccount.register(requestValido()))
                    .isInstanceOf(CpfAlreadyExistsException.class);

            verify(identityProvider, never()).deleteUser(anyString());
        }
    }
}
