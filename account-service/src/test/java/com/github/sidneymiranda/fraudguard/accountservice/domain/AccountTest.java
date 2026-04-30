package com.github.sidneymiranda.fraudguard.accountservice.domain;

import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.DomainException;
import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.InvalidCpfException;
import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.InvalidEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Account — Entidade de Domínio")
class AccountTest {

    // ─── fixtures ─────────────────────────────────────────────────────────────

    static final UUID   PROVIDER_ID  = UUID.randomUUID();
    static final String FULL_NAME    = "Sidney Miranda";
    static final String CPF_VALIDO   = "52998224725";
    static final String EMAIL_VALIDO = "sidney@example.com";

    /** Builder pré-configurado com todos os campos obrigatórios. */
    private Account.AccountBuilder builderValido() {
        return Account.builder()
                .id(PROVIDER_ID)
                .fullName(FULL_NAME)
                .cpf(CPF_VALIDO)
                .email(EMAIL_VALIDO);
    }

    // ─── build — caminho feliz ─────────────────────────────────────────────────

    @Nested
    @DisplayName("builder().build() — criação bem-sucedida")
    class CaminhoFeliz {

        @Test
        @DisplayName("cria Account com todos os campos obrigatórios preenchidos")
        void deveCriarAccountValida() {
            Account account = builderValido().build();

            assertThat(account.getId()).isEqualTo(PROVIDER_ID);
            assertThat(account.getFullName()).isEqualTo(FULL_NAME);
            assertThat(account.getCpf().value()).isEqualTo(CPF_VALIDO);
            assertThat(account.getEmail().value()).isEqualTo(EMAIL_VALIDO);
        }

        @Test
        @DisplayName("accountType padrão é PERSONAL quando omitido")
        void deveUsarAccountTypePersonalPorPadrao() {
            Account account = builderValido().build();
            assertThat(account.getAccountType()).isEqualTo(AccountType.PERSONAL);
        }

        @Test
        @DisplayName("accountType BUSINESS é respeitado quando informado")
        void deveAceitarAccountTypeBusiness() {
            Account account = builderValido().accountType(AccountType.BUSINESS).build();
            assertThat(account.getAccountType()).isEqualTo(AccountType.BUSINESS);
        }

        @Test
        @DisplayName("createdAt é preenchido automaticamente quando omitido")
        void devePreencherCreatedAtAutomaticamente() {
            Instant antes = Instant.now();
            Account account = builderValido().build();
            Instant depois = Instant.now();

            assertThat(account.getCreatedAt())
                    .isAfterOrEqualTo(antes)
                    .isBeforeOrEqualTo(depois);
        }

        @Test
        @DisplayName("createdAt explícito é respeitado")
        void deveUsarCreatedAtExplicito() {
            Instant createdAt = Instant.parse("2024-01-15T10:00:00Z");
            Account account = builderValido().createdAt(createdAt).build();
            assertThat(account.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("updatedAt é inicializado igual ao createdAt")
        void deveInicializarUpdatedAtIgualAoCreatedAt() {
            Account account = builderValido().build();
            assertThat(account.getUpdatedAt()).isEqualTo(account.getCreatedAt());
        }

        @Test
        @DisplayName("fullName é armazenado sem espaços extras nas bordas")
        void deveTrimFullName() {
            Account account = builderValido().fullName("  Sidney Miranda  ").build();
            assertThat(account.getFullName()).isEqualTo("Sidney Miranda");
        }
    }

    // ─── build — campos obrigatórios ──────────────────────────────────────────

    @Nested
    @DisplayName("builder().build() — campos obrigatórios ausentes")
    class CamposObrigatorios {

        @Test
        @DisplayName("rejeita build sem id — obrigatório vir do provedor de identidade")
        void deveRejeitarSemId() {
            assertThatThrownBy(() ->
                    Account.builder()
                            .fullName(FULL_NAME)
                            .cpf(CPF_VALIDO)
                            .email(EMAIL_VALIDO)
                            .build()
            ).isInstanceOf(NullPointerException.class)
             .hasMessageContaining("provedor de identidade");
        }

        @Test
        @DisplayName("rejeita build sem cpf")
        void deveRejeitarSemCpf() {
            assertThatThrownBy(() ->
                    Account.builder()
                            .id(PROVIDER_ID)
                            .fullName(FULL_NAME)
                            .email(EMAIL_VALIDO)
                            .build()
            ).isInstanceOf(NullPointerException.class)
             .hasMessageContaining("CPF");
        }

        @Test
        @DisplayName("rejeita build sem email")
        void deveRejeitarSemEmail() {
            assertThatThrownBy(() ->
                    Account.builder()
                            .id(PROVIDER_ID)
                            .fullName(FULL_NAME)
                            .cpf(CPF_VALIDO)
                            .build()
            ).isInstanceOf(NullPointerException.class)
             .hasMessageContaining("E-mail");
        }

        @Test
        @DisplayName("rejeita build sem fullName")
        void deveRejeitarSemFullName() {
            assertThatThrownBy(() ->
                    Account.builder()
                            .id(PROVIDER_ID)
                            .cpf(CPF_VALIDO)
                            .email(EMAIL_VALIDO)
                            .build()
            ).isInstanceOf(DomainException.class);
        }
    }

    // ─── build — validações de formato ────────────────────────────────────────

    @Nested
    @DisplayName("builder().build() — validações de formato")
    class ValidacoesDeFormato {

        @Test
        @DisplayName("rejeita fullName em branco")
        void deveRejeitarFullNameEmBranco() {
            assertThatThrownBy(() -> builderValido().fullName("   ").build())
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("rejeita fullName com menos de 2 caracteres")
        void deveRejeitarFullNameMuitoCurto() {
            assertThatThrownBy(() -> builderValido().fullName("A").build())
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("rejeita fullName com 1 caractere após trim")
        void deveRejeitarFullNameComUmCaractereAposTrim() {
            // " A " tem 1 char após trim → deve falhar
            assertThatThrownBy(() -> builderValido().fullName("  A  ").build())
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("aceita fullName com exatamente 2 caracteres após trim")
        void deveAceitarFullNameComDoisCaracteres() {
            Account account = builderValido().fullName("Ai").build();
            assertThat(account.getFullName()).isEqualTo("Ai");
        }

        @Test
        @DisplayName("rejeita CPF inválido no build")
        void deveRejeitarCpfInvalidoNoBuild() {
            assertThatThrownBy(() -> builderValido().cpf("00000000000").build())
                    .isInstanceOf(InvalidCpfException.class);
        }

        @Test
        @DisplayName("rejeita e-mail inválido no build")
        void deveRejeitarEmailInvalidoNoBuild() {
            assertThatThrownBy(() -> builderValido().email("nao-e-um-email").build())
                    .isInstanceOf(InvalidEmailException.class);
        }
    }

    // ─── updateProfile ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProfile() — RF-04")
    class UpdateProfile {

        @Test
        @DisplayName("atualiza nome e e-mail com sucesso")
        void deveAtualizarNomeEEmail() {
            Account account = builderValido().build();
            Instant antesDoUpdate = account.getUpdatedAt();

            account.updateProfile("Novo Nome", "novo@example.com");

            assertThat(account.getFullName()).isEqualTo("Novo Nome");
            assertThat(account.getEmail().value()).isEqualTo("novo@example.com");
            assertThat(account.getUpdatedAt()).isAfterOrEqualTo(antesDoUpdate);
        }

        @Test
        @DisplayName("CPF permanece imutável após updateProfile")
        void deveManterCpfImutavel() {
            Account account = builderValido().build();
            CPF cpfOriginal = account.getCpf();

            account.updateProfile("Novo Nome", "novo@example.com");

            assertThat(account.getCpf()).isEqualTo(cpfOriginal);
        }

        @Test
        @DisplayName("rejeita fullName null no updateProfile")
        void deveRejeitarFullNameNullNoUpdate() {
            Account account = builderValido().build();
            assertThatThrownBy(() -> account.updateProfile(null, "novo@example.com"))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("rejeita fullName em branco no updateProfile")
        void deveRejeitarFullNameEmBrancoNoUpdate() {
            Account account = builderValido().build();
            assertThatThrownBy(() -> account.updateProfile("   ", "novo@example.com"))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("rejeita e-mail inválido no updateProfile")
        void deveRejeitarEmailInvalidoNoUpdate() {
            Account account = builderValido().build();
            assertThatThrownBy(() -> account.updateProfile("Novo Nome", "invalido"))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("updatedAt avança após cada update")
        void deveAvancarUpdatedAt() throws InterruptedException {
            Account account = builderValido().build();
            Instant primeiroUpdate = account.getUpdatedAt();

            Thread.sleep(2); // garante diferença temporal
            account.updateProfile("Nome Atualizado", "atualizado@example.com");

            assertThat(account.getUpdatedAt()).isAfter(primeiroUpdate);
        }
    }

    // ─── hasSameCpf / hasSameEmail ────────────────────────────────────────────

    @Nested
    @DisplayName("hasSameCpf() e hasSameEmail()")
    class PredicadosDeUnicidade {

        @Test
        @DisplayName("hasSameCpf() retorna true para o mesmo CPF")
        void hasSameCpfRetornaTrueParaMesmoCpf() {
            Account account = builderValido().build();
            assertThat(account.hasSameCpf(CPF.of(CPF_VALIDO))).isTrue();
        }

        @Test
        @DisplayName("hasSameCpf() retorna false para CPF diferente")
        void hasSameCpfRetornaFalseParaCpfDiferente() {
            Account account = builderValido().build();
            assertThat(account.hasSameCpf(CPF.of("11144477735"))).isFalse();
        }

        @Test
        @DisplayName("hasSameEmail() retorna true para o mesmo e-mail (case-insensitive)")
        void hasSameEmailRetornaTrueParaMesmoEmail() {
            Account account = builderValido().build();
            assertThat(account.hasSameEmail(Email.of("SIDNEY@EXAMPLE.COM"))).isTrue();
        }

        @Test
        @DisplayName("hasSameEmail() retorna false para e-mail diferente")
        void hasSameEmailRetornaFalseParaEmailDiferente() {
            Account account = builderValido().build();
            assertThat(account.hasSameEmail(Email.of("outro@example.com"))).isFalse();
        }
    }

    // ─── identidade e reconstitution ──────────────────────────────────────────

    @Nested
    @DisplayName("equals(), hashCode() e reconstitute()")
    class IdentidadeEReconstituicao {

        @Test
        @DisplayName("duas Accounts com mesmo CPF são iguais (identidade por CPF)")
        void deveSerIgualParaMesmoCpf() {
            Account a1 = builderValido().id(UUID.randomUUID()).build();
            Account a2 = builderValido().id(UUID.randomUUID()).email("outro@email.com").build();

            assertThat(a1).isEqualTo(a2);
            assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        }

        @Test
        @DisplayName("duas Accounts com CPFs diferentes não são iguais")
        void deveSerDiferenteParaCpfsDiferentes() {
            Account a1 = builderValido().build();
            Account a2 = builderValido().cpf("11144477735").build();

            assertThat(a1).isNotEqualTo(a2);
        }

        @Test
        @DisplayName("reflexividade: Account é igual a si mesma (this == o)")
        void reflexividade() {
            Account account = builderValido().build();
            assertThat(account).isEqualTo(account);
        }

        @Test
        @DisplayName("Account não é igual a null")
        void naoIgualANull() {
            Account account = builderValido().build();
            assertThat(account).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Account não é igual a objeto de tipo diferente")
        void naoIgualATipoDiferente() {
            Account account = builderValido().build();
            assertThat(account).isNotEqualTo("um-string-qualquer");
        }

        @Test
        @DisplayName("reconstitute() cria Account equivalente à original")
        void deveReconstituirAccountEquivalente() {
            Instant createdAt = Instant.parse("2024-06-01T12:00:00Z");
            Instant updatedAt = Instant.parse("2024-06-10T08:00:00Z");

            Account original = builderValido().createdAt(createdAt).build();
            Account reconstituida = Account.reconstitute(
                    PROVIDER_ID, FULL_NAME, CPF_VALIDO, EMAIL_VALIDO,
                    AccountType.PERSONAL, createdAt, updatedAt
            );

            assertThat(reconstituida.getId()).isEqualTo(PROVIDER_ID);
            assertThat(reconstituida.getCpf()).isEqualTo(original.getCpf());
            assertThat(reconstituida.getUpdatedAt()).isEqualTo(updatedAt);
        }

        @Test
        @DisplayName("toString() não expõe CPF completo nem senha")
        void toStringSemDadosSensiveis() {
            Account account = builderValido().build();
            String repr = account.toString();

            assertThat(repr).doesNotContain(CPF_VALIDO);
            assertThat(repr).doesNotContain("password");
            assertThat(repr).doesNotContain("senha");
            assertThat(repr).contains("Account{");
        }
    }
}

