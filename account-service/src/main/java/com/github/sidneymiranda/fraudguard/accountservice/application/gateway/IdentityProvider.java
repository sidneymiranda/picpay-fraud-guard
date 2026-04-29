package com.github.sidneymiranda.fraudguard.accountservice.application.gateway;

/**
 * Porta driven para o provedor de identidade — abstração da camada de aplicação.
 *
 * <p>Isola o restante do sistema de todos os detalhes do provedor de identidade
 * (protocolo, SDK, realm, etc.). A implementação concreta fica em
 * {@code infrastructure/identity} e pode ser substituída sem afetar nenhuma
 * regra de negócio (ex.: Keycloak → Auth0 → Cognito).
 *
 * <p>Segue o mesmo padrão de abstração de {@code AccountRepository} e
 * {@code EventPublisher}: o caso de uso depende do <em>papel</em>, não do produto.
 */
public interface IdentityProvider {

    /**
     * Cria um usuário no provedor de identidade e retorna o ID gerado.
     *
     * <p>A senha é enviada diretamente ao provedor — não é armazenada no
     * {@code account-service} (RNF-04).
     *
     * @param email    e-mail do usuário — utilizado como username no provedor
     * @param password senha em texto plano — transitória, nunca persiste no domínio
     * @param fullName nome completo do usuário
     * @return userId gerado pelo provedor como String (UUID) — será o ID da {@code Account}
     */
    String createUser(String email, String password, String fullName);

    /**
     * Remove o usuário do provedor de identidade.
     *
     * <p>Usado exclusivamente para compensação no padrão SAGA: chamado quando o cadastro
     * falha após o usuário já ter sido criado no provedor, garantindo consistência distribuída.
     *
     * @param userId ID do usuário no provedor, retornado por {@link #createUser}
     */
    void deleteUser(String userId);
}
