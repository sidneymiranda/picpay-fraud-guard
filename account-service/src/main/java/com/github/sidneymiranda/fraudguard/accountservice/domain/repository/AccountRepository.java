package com.github.sidneymiranda.fraudguard.accountservice.domain.repository;

import com.github.sidneymiranda.fraudguard.accountservice.domain.Account;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistência para a entidade {@link Account}.
 *
 * <p>Segue o princípio de inversão de dependências (DIP):
 * a interface é definida no domínio — a implementação concreta fica em {@code infrastructure/repository}.
 * O domínio nunca depende de detalhes de infraestrutura (JPA, SQL, etc.).
 */
public interface AccountRepository {

    /**
     * Persiste ou atualiza uma {@link Account}.
     *
     * @param account entidade a ser salva
     * @return a entidade salva — pode refletir campos gerenciados pelo banco (timestamps, etc.)
     */
    Account save(Account account);

    /**
     * Recupera uma {@link Account} pelo ID do usuário.
     *
     * @param id UUID do usuário — mesmo ID retornado pelo Keycloak
     */
    Optional<Account> findById(UUID id);

    /**
     * Recupera uma {@link Account} pelo CPF.
     * Útil para reconstituição de entidade — ex: atualizar perfil via CPF.
     *
     * @param cpf CPF sem formatação (11 dígitos)
     */
    Optional<Account> findByCpf(String cpf);

    /**
     * Verifica se já existe uma {@link Account} com o CPF informado.
     * Mais eficiente que {@link #findByCpf} para verificações de unicidade (RF-01).
     *
     * @param cpf CPF sem formatação (11 dígitos)
     */
    boolean existsByCpf(String cpf);

    /**
     * Verifica se já existe uma {@link Account} com o e-mail informado.
     * Usado para validação de unicidade antes de persistir (RF-01).
     *
     * @param email e-mail normalizado (lowercase)
     */
    boolean existsByEmail(String email);
}

