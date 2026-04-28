package com.github.sidneymiranda.fraudguard.accountservice.domain;

import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.InvalidCpfException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CPF — Value Object")
class CPFTest {

    // CPFs válidos usados nos testes
    static final String CPF_VALIDO          = "52998224725";   // dígitos
    static final String CPF_VALIDO_MASK     = "529.982.247-25"; // com máscara
    static final String CPF_VALIDO_2        = "11144477735";

    // ─── criação válida ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("CPF.of() — criação válida")
    class CriacaoValida {

        @Test
        @DisplayName("aceita CPF sem formatação")
        void deveAceitarCpfSemFormatacao() {
            CPF cpf = CPF.of(CPF_VALIDO);
            assertThat(cpf.value()).isEqualTo(CPF_VALIDO);
        }

        @Test
        @DisplayName("aceita CPF com máscara (pontos e traço)")
        void deveAceitarCpfComMascara() {
            CPF cpf = CPF.of(CPF_VALIDO_MASK);
            assertThat(cpf.value()).isEqualTo(CPF_VALIDO);
        }

        @Test
        @DisplayName("aceita CPF com espaços extras ao redor")
        void deveAceitarCpfComEspacos() {
            CPF cpf = CPF.of("  " + CPF_VALIDO_MASK + "  ");
            assertThat(cpf.value()).isEqualTo(CPF_VALIDO);
        }

        @Test
        @DisplayName("aceita CPF cujo 2º dígito verificador é calculado via remainder == 10")
        void deveAceitarCpfComSegundoDigitoViaRemainder10() {
            // CPF 123.456.788-10: sum₂ * 10 % 11 = 10 → expected = 0 (branch "remainder == 10")
            // Verificação:
            //   isValidFirstDigit:  sum=208, (208*10)%11=1  → 1º dígito = 1  ✓
            //   isValidSecondDigit: sum=254, (254*10)%11=10 → expected=0, 2º dígito = 0 ✓
            CPF cpf = CPF.of("12345678810");
            assertThat(cpf.value()).isEqualTo("12345678810");
        }
    }

    // ─── rejeição de CPFs inválidos ───────────────────────────────────────────

    @Nested
    @DisplayName("CPF.of() — rejeição de entradas inválidas")
    class RejeicaoInvalidos {

        @Test
        @DisplayName("rejeita null")
        void deveRejeitarNull() {
            assertThatThrownBy(() -> CPF.of(null))
                    .isInstanceOf(InvalidCpfException.class);
        }

        @Test
        @DisplayName("rejeita string vazia")
        void deveRejeitarVazio() {
            assertThatThrownBy(() -> CPF.of(""))
                    .isInstanceOf(InvalidCpfException.class);
        }

        @Test
        @DisplayName("rejeita string apenas com espaços")
        void deveRejeitarApenasEspacos() {
            assertThatThrownBy(() -> CPF.of("   "))
                    .isInstanceOf(InvalidCpfException.class);
        }

        @ParameterizedTest(name = "CPF com todos os dígitos iguais: {0}")
        @DisplayName("rejeita CPFs com dígitos todos iguais")
        @ValueSource(strings = {
                "00000000000", "11111111111", "22222222222",
                "33333333333", "44444444444", "55555555555",
                "66666666666", "77777777777", "88888888888", "99999999999"
        })
        void deveRejeitarDigitosIguais(String cpf) {
            assertThatThrownBy(() -> CPF.of(cpf))
                    .isInstanceOf(InvalidCpfException.class);
        }

        @Test
        @DisplayName("rejeita CPF com menos de 11 dígitos")
        void deveRejeitarMenosDeOnzeDigitos() {
            assertThatThrownBy(() -> CPF.of("1234567890"))   // 10 dígitos
                    .isInstanceOf(InvalidCpfException.class);
        }

        @Test
        @DisplayName("rejeita CPF com mais de 11 dígitos")
        void deveRejeitarMaisDeOnzeDigitos() {
            assertThatThrownBy(() -> CPF.of("123456789012"))  // 12 dígitos
                    .isInstanceOf(InvalidCpfException.class);
        }

        @Test
        @DisplayName("rejeita CPF com primeiro dígito verificador incorreto")
        void deveRejeitarPrimeiroDigitoVerificadorIncorreto() {
            // 529.982.247-25 é válido; trocamos o 1º dígito verificador de 2 para 9
            assertThatThrownBy(() -> CPF.of("52998224795"))
                    .isInstanceOf(InvalidCpfException.class);
        }

        @Test
        @DisplayName("rejeita CPF com segundo dígito verificador incorreto")
        void deveRejeitarSegundoDigitoVerificadorIncorreto() {
            // 529.982.247-25 é válido; trocamos o 2º dígito verificador de 5 para 9
            assertThatThrownBy(() -> CPF.of("52998224729"))
                    .isInstanceOf(InvalidCpfException.class);
        }

        @Test
        @DisplayName("rejeita CPF sequencial (1234567890x)")
        void deveRejeitarCpfSequencial() {
            assertThatThrownBy(() -> CPF.of("12345678900"))
                    .isInstanceOf(InvalidCpfException.class);
        }
    }

    // ─── comportamentos ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("formatted() e toString()")
    class Representacao {

        @Test
        @DisplayName("formatted() retorna CPF no padrão XXX.XXX.XXX-XX")
        void deveRetornarCpfFormatado() {
            CPF cpf = CPF.of(CPF_VALIDO);
            assertThat(cpf.formatted()).isEqualTo(CPF_VALIDO_MASK);
        }

        @Test
        @DisplayName("toString() mascara o CPF — nunca expõe dígitos completos")
        void deveRetornarCpfMascarado() {
            CPF cpf = CPF.of(CPF_VALIDO);
            String mascarado = cpf.toString();
            assertThat(mascarado).doesNotContain(CPF_VALIDO);
            assertThat(mascarado).startsWith("***");
            assertThat(mascarado).endsWith("**");
        }
    }

    // ─── identidade ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("equals() e hashCode()")
    class Identidade {

        @Test
        @DisplayName("CPFs com mesmo valor são iguais independente da formatação de entrada")
        void deveSerIgualParaMesmoValor() {
            CPF semMascara = CPF.of(CPF_VALIDO);
            CPF comMascara = CPF.of(CPF_VALIDO_MASK);
            assertThat(semMascara).isEqualTo(comMascara);
            assertThat(semMascara.hashCode()).isEqualTo(comMascara.hashCode());
        }

        @Test
        @DisplayName("CPFs com valores diferentes não são iguais")
        void deveSerDiferenteParaValoresDiferentes() {
            CPF cpf1 = CPF.of(CPF_VALIDO);
            CPF cpf2 = CPF.of(CPF_VALIDO_2);
            assertThat(cpf1).isNotEqualTo(cpf2);
        }

        @Test
        @DisplayName("reflexividade: CPF é igual a si mesmo")
        void reflexividade() {
            CPF cpf = CPF.of(CPF_VALIDO);
            assertThat(cpf).isEqualTo(cpf);
        }

        @Test
        @DisplayName("CPF não é igual a null")
        void naoIgualANull() {
            CPF cpf = CPF.of(CPF_VALIDO);
            assertThat(cpf).isNotEqualTo(null);
        }
    }
}

