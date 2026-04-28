package com.github.sidneymiranda.fraudguard.accountservice.domain;

import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.InvalidEmailException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object que representa um endereço de e-mail válido.
 * Imutável e comparável por valor — RF-04: e-mail é atualizável via updateProfile().
 */
public final class Email {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    /**
     * Factory method que valida e cria um Email.
     *
     * @throws InvalidEmailException se o formato for inválido
     */
    public static Email of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidEmailException(raw);
        }
        String normalized = raw.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidEmailException(raw);
        }
        return new Email(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email email)) return false;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

