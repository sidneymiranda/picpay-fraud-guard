package com.github.sidneymiranda.fraudguard.accountservice.domain;

import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.InvalidCpfException;

import java.util.Objects;

/**
 * Value Object que representa um CPF brasileiro válido.
 * Imutável por design — RF-04: CPF nunca pode ser alterado após criação.
 */
public final class CPF {

    private static final int CPF_LENGTH = 11;
    private final String value;

    private CPF(String value) {
        this.value = value;
    }

    /**
     * Factory method que valida e cria um CPF.
     *
     * @param raw CPF com ou sem formatação (ex: "123.456.789-09" ou "12345678909")
     * @throws InvalidCpfException se o CPF for inválido
     */
    public static CPF of(String raw) {
        String digits = sanitize(raw);
        validate(digits);
        return new CPF(digits);
    }

    /** Retorna os 11 dígitos sem formatação. */
    public String value() {
        return value;
    }

    /** Retorna CPF formatado como "XXX.XXX.XXX-XX". */
    public String formatted() {
        return String.format("%s.%s.%s-%s",
                value.substring(0, 3),
                value.substring(3, 6),
                value.substring(6, 9),
                value.substring(9, 11));
    }

    // ─── validação ───────────────────────────────────────────────────────────

    private static String sanitize(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^0-9]", "");
    }

    private static void validate(String digits) {
        if (digits.length() != CPF_LENGTH || hasAllSameDigits(digits)) {
            throw new InvalidCpfException(digits);
        }
        if (!isValidFirstDigit(digits) || !isValidSecondDigit(digits)) {
            throw new InvalidCpfException(digits);
        }
    }

    private static boolean hasAllSameDigits(String digits) {
        return digits.chars().allMatch(c -> c == digits.charAt(0));
    }

    private static boolean isValidFirstDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * (10 - i);
        }
        int remainder = (sum * 10) % 11;
        int expected = remainder == 10 ? 0 : remainder;
        return expected == Character.getNumericValue(digits.charAt(9));
    }

    private static boolean isValidSecondDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * (11 - i);
        }
        int remainder = (sum * 10) % 11;
        int expected = remainder == 10 ? 0 : remainder;
        return expected == Character.getNumericValue(digits.charAt(10));
    }

    // ─── equals / hashCode / toString ────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CPF cpf)) return false;
        return Objects.equals(value, cpf.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /** Retorna CPF mascarado (ex: "***456789**") para logs seguros. */
    @Override
    public String toString() {
        return "***" + value.substring(3, 9) + "**";
    }
}

