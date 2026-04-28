package com.github.sidneymiranda.fraudguard.accountservice.domain;

import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.DomainException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio que representa uma conta de usuário.
 *
 * <p>Regras de negócio encapsuladas:
 * <ul>
 *   <li>RF-01 — CPF válido e e-mail obrigatórios no registro</li>
 *   <li>RF-04 — apenas nome completo e e-mail são atualizáveis; CPF é imutável</li>
 *   <li>RNF-04 — senha nunca é armazenada nesta entidade (delegada ao Keycloak)</li>
 * </ul>
 *
 * <p>Uso típico na camada de aplicação (id vem do Keycloak):
 * <pre>{@code
 * String keycloakUserId = keycloakClient.registerUser(request); // obrigatório — sem isso, Account não é criada
 * Account account = Account.builder()
 *     .id(UUID.fromString(keycloakUserId))  // userId retornado pelo Keycloak — obrigatório
 *     .fullName(request.getFullName())
 *     .cpf(request.getCpf())
 *     .email(request.getEmail())
 *     .accountType(AccountType.PERSONAL)
 *     .build();
 * }</pre>
 */
public class Account {

    private final UUID id;
    private String fullName;
    private final CPF cpf;
    private Email email;
    private final AccountType accountType;
    private final Instant createdAt;
    private Instant updatedAt;

    private Account(UUID id, String fullName, CPF cpf, Email email,
                    AccountType accountType, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.cpf = cpf;
        this.email = email;
        this.accountType = accountType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ─── factory methods ──────────────────────────────────────────────────────

    /** Ponto de entrada do Builder. */
    public static AccountBuilder builder() {
        return new AccountBuilder();
    }

    /**
     * Reconstitui uma entidade a partir de dados persistidos (usado pelos repositórios).
     * Bypassa o Builder — não atribui novos defaults.
     */
    public static Account reconstitute(UUID id, String fullName, String cpfDigits,
                                       String emailValue, AccountType accountType,
                                       Instant createdAt, Instant updatedAt) {
        return new Account(id, fullName, CPF.of(cpfDigits), Email.of(emailValue),
                accountType, createdAt, updatedAt);
    }

    // ─── comportamentos de negócio ────────────────────────────────────────────

    /**
     * Atualiza nome completo e/ou e-mail do perfil (RF-04).
     * CPF é imutável — qualquer tentativa de alteração é impossível em nível de design.
     *
     * @throws DomainException se fullName for inválido ou e-mail tiver formato incorreto
     */
    public void updateProfile(String newFullName, String newRawEmail) {
        validateFullName(newFullName);
        this.fullName = newFullName.trim();
        this.email = Email.of(newRawEmail);
        this.updatedAt = Instant.now();
    }

    /**
     * Verifica se esta conta possui o mesmo CPF que o fornecido.
     * Usado pela camada de aplicação para checar unicidade antes de persistir.
     */
    public boolean hasSameCpf(CPF other) {
        return this.cpf.equals(other);
    }

    /**
     * Verifica se esta conta possui o mesmo e-mail que o fornecido.
     */
    public boolean hasSameEmail(Email other) {
        return this.email.equals(other);
    }

    // ─── validações internas ──────────────────────────────────────────────────

    private static void validateFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new DomainException("Nome completo é obrigatório") {};
        }
        if (fullName.trim().length() < 2) {
            throw new DomainException("Nome completo deve ter pelo menos 2 caracteres") {};
        }
    }

    // ─── getters (somente leitura — sem setters públicos) ────────────────────

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    /** Retorna o CPF como Value Object — imutável. */
    public CPF getCpf() {
        return cpf;
    }

    public Email getEmail() {
        return email;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // ─── identidade baseada em CPF ────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return Objects.equals(cpf, account.cpf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpf);
    }

    /** Nunca expõe dados sensíveis. CPF é mascarado via CPF.toString(). */
    @Override
    public String toString() {
        return "Account{id=" + id + ", fullName='" + fullName + "', cpf=" + cpf +
               ", email=" + email + ", accountType=" + accountType +
               ", createdAt=" + createdAt + "}";
    }

    // ─── Builder ──────────────────────────────────────────────────────────────

    public static final class AccountBuilder {

        private UUID id;
        private String fullName;
        private String cpf;
        private String email;
        private AccountType accountType;
        private Instant createdAt;

        private AccountBuilder() {}

        /**
         * ID da conta — obrigatório. Deve ser o ID retornado pelo Keycloak após
         * registro bem-sucedido no realm da aplicação.
         */
        public AccountBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        /** Nome completo do titular — obrigatório. */
        public AccountBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        /** CPF em formato livre (com ou sem máscara) — obrigatório. */
        public AccountBuilder cpf(String cpf) {
            this.cpf = cpf;
            return this;
        }

        /** E-mail do titular — obrigatório. */
        public AccountBuilder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Tipo de conta — opcional.
         * Default: {@link AccountType#PERSONAL}.
         */
        public AccountBuilder accountType(AccountType accountType) {
            this.accountType = accountType;
            return this;
        }

        /**
         * Data de criação — opcional.
         * Default: {@code Instant.now()} no momento do {@link #build()}.
         */
        public AccountBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Valida os campos obrigatórios, aplica defaults e constrói a entidade.
         *
         * @throws NullPointerException se id, cpf, email ou fullName forem nulos
         * @throws DomainException      se cpf, email ou fullName forem inválidos
         */
        public Account build() {
            Objects.requireNonNull(id, "ID é obrigatório — deve ser o userId retornado pelo Keycloak");
            Objects.requireNonNull(cpf, "CPF é obrigatório");
            Objects.requireNonNull(email, "E-mail é obrigatório");

            validateFullName(fullName);

            AccountType resolvedType = (accountType != null) ? accountType : AccountType.PERSONAL;
            Instant resolvedCreated  = (createdAt != null)   ? createdAt   : Instant.now();

            CPF resolvedCpf     = CPF.of(cpf);
            Email resolvedEmail = Email.of(email);

            return new Account(
                    id,
                    fullName.trim(),
                    resolvedCpf,
                    resolvedEmail,
                    resolvedType,
                    resolvedCreated,
                    resolvedCreated
            );
        }
    }
}

