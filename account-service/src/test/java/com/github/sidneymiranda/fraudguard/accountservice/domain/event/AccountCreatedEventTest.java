package com.github.sidneymiranda.fraudguard.accountservice.domain.event;

import com.github.sidneymiranda.fraudguard.accountservice.domain.Account;
import com.github.sidneymiranda.fraudguard.accountservice.domain.AccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AccountCreatedEvent — Evento de Domínio (RF-03)")
class AccountCreatedEventTest {

    // ─── fixtures ─────────────────────────────────────────────────────────────

    static final UUID        USER_ID    = UUID.randomUUID();
    static final String      EMAIL      = "sid@example.com";
    static final Instant     CREATED_AT = Instant.parse("2024-06-01T10:00:00Z");
    static final AccountType TYPE       = AccountType.PERSONAL;

    private Account accountValido() {
        return Account.builder()
                .id(USER_ID)
                .fullName("Sidney Miranda")
                .cpf("52998224725")
                .email(EMAIL)
                .accountType(TYPE)
                .createdAt(CREATED_AT)
                .build();
    }

    // ─── construção via construtor canônico ───────────────────────────────────

    @Nested
    @DisplayName("construtor canônico — validações de invariante")
    class ConstrucaoDireta {

        @Test
        @DisplayName("cria evento com todos os campos válidos")
        void deveCriarEventoValido() {
            AccountCreatedEvent event = new AccountCreatedEvent(USER_ID, EMAIL, CREATED_AT, TYPE);

            assertThat(event.userId()).isEqualTo(USER_ID);
            assertThat(event.email()).isEqualTo(EMAIL);
            assertThat(event.createdAt()).isEqualTo(CREATED_AT);
            assertThat(event.accountType()).isEqualTo(TYPE);
        }

        @Test
        @DisplayName("rejeita userId nulo — campo obrigatório do payload RF-03")
        void deveRejeitarUserIdNulo() {
            assertThatThrownBy(() -> new AccountCreatedEvent(null, EMAIL, CREATED_AT, TYPE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("rejeita email nulo — campo obrigatório do payload RF-03")
        void deveRejeitarEmailNulo() {
            assertThatThrownBy(() -> new AccountCreatedEvent(USER_ID, null, CREATED_AT, TYPE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("rejeita email em branco — evento com e-mail vazio não representa intenção válida")
        void deveRejeitarEmailEmBranco() {
            assertThatThrownBy(() -> new AccountCreatedEvent(USER_ID, "   ", CREATED_AT, TYPE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejeita createdAt nulo — campo obrigatório do payload RF-03")
        void deveRejeitarCreatedAtNulo() {
            assertThatThrownBy(() -> new AccountCreatedEvent(USER_ID, EMAIL, null, TYPE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("createdAt");
        }

        @Test
        @DisplayName("rejeita accountType nulo — campo obrigatório do payload RF-03")
        void deveRejeitarAccountTypeNulo() {
            assertThatThrownBy(() -> new AccountCreatedEvent(USER_ID, EMAIL, CREATED_AT, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("accountType");
        }
    }

    // ─── factory method from(Account) ────────────────────────────────────────

    @Nested
    @DisplayName("AccountCreatedEvent.from(Account) — factory method")
    class FactoryMethod {

        @Test
        @DisplayName("mapeia todos os campos da Account corretamente")
        void deveMapeiarTodosOsCampos() {
            Account account = accountValido();

            AccountCreatedEvent event = AccountCreatedEvent.from(account);

            assertThat(event.userId()).isEqualTo(account.getId());
            assertThat(event.email()).isEqualTo(account.getEmail().value());
            assertThat(event.createdAt()).isEqualTo(account.getCreatedAt());
            assertThat(event.accountType()).isEqualTo(account.getAccountType());
        }

        @Test
        @DisplayName("email do evento é o valor normalizado do Value Object Email")
        void emailDeveSerOValorNormalizado() {
            // Email.of() normaliza para lowercase
            Account account = Account.builder()
                    .id(USER_ID)
                    .fullName("Sidney Miranda")
                    .cpf("52998224725")
                    .email("UPPER@EXAMPLE.COM")   // será normalizado para lowercase
                    .accountType(TYPE)
                    .build();

            AccountCreatedEvent event = AccountCreatedEvent.from(account);

            assertThat(event.email()).isEqualTo("upper@example.com");
        }

        @Test
        @DisplayName("accountType BUSINESS é propagado corretamente para o evento")
        void devePropagarAccountTypeBusiness() {
            Account account = Account.builder()
                    .id(USER_ID)
                    .fullName("Sidney Miranda")
                    .cpf("52998224725")
                    .email(EMAIL)
                    .accountType(AccountType.BUSINESS)
                    .build();

            AccountCreatedEvent event = AccountCreatedEvent.from(account);

            assertThat(event.accountType()).isEqualTo(AccountType.BUSINESS);
        }
    }

    // ─── imutabilidade e identidade (record) ─────────────────────────────────

    @Nested
    @DisplayName("imutabilidade e identidade — garantias do record")
    class ImutabilidadeEIdentidade {

        @Test
        @DisplayName("dois eventos com os mesmos dados são iguais (record equals)")
        void doisEventosComMesDadosSaoIguais() {
            AccountCreatedEvent e1 = new AccountCreatedEvent(USER_ID, EMAIL, CREATED_AT, TYPE);
            AccountCreatedEvent e2 = new AccountCreatedEvent(USER_ID, EMAIL, CREATED_AT, TYPE);

            assertThat(e1).isEqualTo(e2);
            assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        }

        @Test
        @DisplayName("eventos com userId diferente não são iguais")
        void eventoComUserIdDiferenteNaoEhIgual() {
            AccountCreatedEvent e1 = new AccountCreatedEvent(USER_ID, EMAIL, CREATED_AT, TYPE);
            AccountCreatedEvent e2 = new AccountCreatedEvent(UUID.randomUUID(), EMAIL, CREATED_AT, TYPE);

            assertThat(e1).isNotEqualTo(e2);
        }

        @Test
        @DisplayName("toString() não expõe dados sensíveis além do previsto")
        void toStringNaoDeveExporSenha() {
            AccountCreatedEvent event = new AccountCreatedEvent(USER_ID, EMAIL, CREATED_AT, TYPE);
            String repr = event.toString();

            assertThat(repr).doesNotContain("password");
            assertThat(repr).doesNotContain("senha");
            // campos esperados no toString do record
            assertThat(repr).contains(USER_ID.toString());
            assertThat(repr).contains(EMAIL);
        }
    }
}

