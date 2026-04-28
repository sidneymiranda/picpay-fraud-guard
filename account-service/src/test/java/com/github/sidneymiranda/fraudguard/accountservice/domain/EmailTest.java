package com.github.sidneymiranda.fraudguard.accountservice.domain;

import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.InvalidEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Email — Value Object")
class EmailTest {

    static final String EMAIL_VALIDO   = "usuario@example.com";
    static final String EMAIL_VALIDO_2 = "outro@dominio.com.br";

    // ─── criação válida ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Email.of() — criação válida")
    class CriacaoValida {

        @Test
        @DisplayName("aceita e-mail simples em minúsculo")
        void deveAceitarEmailSimples() {
            Email email = Email.of(EMAIL_VALIDO);
            assertThat(email.value()).isEqualTo(EMAIL_VALIDO);
        }

        @Test
        @DisplayName("normaliza e-mail para letras minúsculas")
        void deveNormalizarParaMinusculo() {
            Email email = Email.of("Usuario@EXAMPLE.COM");
            assertThat(email.value()).isEqualTo("usuario@example.com");
        }

        @Test
        @DisplayName("remove espaços no início e no fim")
        void deveRemoverEspacos() {
            Email email = Email.of("  " + EMAIL_VALIDO + "  ");
            assertThat(email.value()).isEqualTo(EMAIL_VALIDO);
        }

        @ParameterizedTest(name = "e-mail válido: {0}")
        @DisplayName("aceita variações de e-mails válidos")
        @ValueSource(strings = {
                "user.name@example.com",
                "user+tag@example.com",
                "user-name@sub.domain.com",
                "user123@example.org",
                "a@b.co"
        })
        void deveAceitarVariacoesValidas(String e) {
            assertThat(Email.of(e).value()).isNotBlank();
        }
    }

    // ─── rejeição de e-mails inválidos ────────────────────────────────────────

    @Nested
    @DisplayName("Email.of() — rejeição de entradas inválidas")
    class RejeicaoInvalidos {

        @Test
        @DisplayName("rejeita null")
        void deveRejeitarNull() {
            assertThatThrownBy(() -> Email.of(null))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("rejeita string vazia")
        void deveRejeitarVazio() {
            assertThatThrownBy(() -> Email.of(""))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("rejeita string apenas com espaços")
        void deveRejeitarApenasEspacos() {
            assertThatThrownBy(() -> Email.of("   "))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("rejeita e-mail sem @")
        void deveRejeitarSemArroba() {
            assertThatThrownBy(() -> Email.of("usuarioexample.com"))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("rejeita e-mail sem domínio após @")
        void deveRejeitarSemDominio() {
            assertThatThrownBy(() -> Email.of("usuario@"))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("rejeita e-mail sem TLD (ex: usuario@example)")
        void deveRejeitarSemTld() {
            assertThatThrownBy(() -> Email.of("usuario@example"))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("rejeita e-mail sem parte local (ex: @example.com)")
        void deveRejeitarSemParteLocal() {
            assertThatThrownBy(() -> Email.of("@example.com"))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("rejeita e-mail com espaço no meio")
        void deveRejeitarComEspacoNoMeio() {
            assertThatThrownBy(() -> Email.of("usu ario@example.com"))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("rejeita e-mail com múltiplos @")
        void deveRejeitarComMultiplosArroba() {
            assertThatThrownBy(() -> Email.of("usu@ario@example.com"))
                    .isInstanceOf(InvalidEmailException.class);
        }
    }

    // ─── identidade ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("equals() e hashCode()")
    class Identidade {

        @Test
        @DisplayName("e-mails com mesmo valor (case-insensitive) são iguais")
        void deveSerIgualIndependenteDoCaso() {
            Email lower  = Email.of("usuario@example.com");
            Email upper  = Email.of("USUARIO@EXAMPLE.COM");
            assertThat(lower).isEqualTo(upper);
            assertThat(lower.hashCode()).isEqualTo(upper.hashCode());
        }

        @Test
        @DisplayName("e-mails com valores diferentes não são iguais")
        void deveSerDiferenteParaValoresDiferentes() {
            Email e1 = Email.of(EMAIL_VALIDO);
            Email e2 = Email.of(EMAIL_VALIDO_2);
            assertThat(e1).isNotEqualTo(e2);
        }

        @Test
        @DisplayName("reflexividade: Email é igual a si mesmo (this == o)")
        void reflexividade() {
            Email email = Email.of(EMAIL_VALIDO);
            assertThat(email).isEqualTo(email);
        }

        @Test
        @DisplayName("Email não é igual a null")
        void naoIgualANull() {
            Email email = Email.of(EMAIL_VALIDO);
            assertThat(email).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Email não é igual a objeto de tipo diferente")
        void naoIgualATipoDiferente() {
            Email email = Email.of(EMAIL_VALIDO);
            assertThat(email).isNotEqualTo(EMAIL_VALIDO); // String ≠ Email
        }

        @Test
        @DisplayName("toString() retorna o valor normalizado")
        void toStringSemDadosSensiveis() {
            Email email = Email.of("USUARIO@EXAMPLE.COM");
            assertThat(email.toString()).isEqualTo("usuario@example.com");
        }
    }
}

