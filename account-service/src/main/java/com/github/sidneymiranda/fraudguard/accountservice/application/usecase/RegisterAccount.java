package com.github.sidneymiranda.fraudguard.accountservice.application.usecase;

import com.github.sidneymiranda.fraudguard.accountservice.application.dto.AccountCreatedResponse;
import com.github.sidneymiranda.fraudguard.accountservice.application.dto.RegisterAccountRequest;
import com.github.sidneymiranda.fraudguard.accountservice.application.exception.AccountRegistrationException;
import com.github.sidneymiranda.fraudguard.accountservice.application.gateway.EventPublisher;
import com.github.sidneymiranda.fraudguard.accountservice.application.gateway.IdentityProvider;
import com.github.sidneymiranda.fraudguard.accountservice.domain.Account;
import com.github.sidneymiranda.fraudguard.accountservice.domain.event.AccountCreatedEvent;
import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.CpfAlreadyExistsException;
import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.EmailAlreadyExistsException;
import com.github.sidneymiranda.fraudguard.accountservice.domain.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: registrar uma nova conta de usuário (RF-01, RF-03).
 *
 * <p>Orquestração:
 * <ol>
 *   <li>Valida unicidade de CPF e e-mail (RF-01) — antes de qualquer efeito colateral externo</li>
 *   <li>Cria usuário no Keycloak — senha delegada, nunca armazenada no account-service (RNF-04)</li>
 *   <li>Persiste {@link Account} no PostgreSQL — sem senha</li>
 *   <li>Publica {@link AccountCreatedEvent} no tópico Kafka {@code account.created} (RF-03)</li>
 * </ol>
 *
 * <p><b>Padrão SAGA com compensação:</b> se qualquer etapa após a criação no provedor de identidade
 * falhar, o usuário é removido via {@code identityProviderGateway.deleteUser} para garantir
 * consistência distribuída. A exceção original é encapsulada em {@link AccountRegistrationException}.
 */
@Service
@Transactional
public class RegisterAccount {

    private static final Logger log = LoggerFactory.getLogger(RegisterAccount.class);

    private final AccountRepository repository;
    private final IdentityProvider identityProvider;
    private final EventPublisher eventPublisher;

    public RegisterAccount(AccountRepository repository,
                           IdentityProvider identityProvider,
                           EventPublisher eventPublisher) {
        this.repository = repository;
        this.identityProvider = identityProvider;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registra uma nova conta.
     *
     * @param request dados de registro
     * @return response com o ID da conta criada
     * @throws CpfAlreadyExistsException    se o CPF já estiver cadastrado (RF-01)
     * @throws EmailAlreadyExistsException  se o e-mail já estiver cadastrado (RF-01)
     * @throws AccountRegistrationException se o processo falhar após a criação no Keycloak
     */
    public AccountCreatedResponse register(RegisterAccountRequest request) {

        if (repository.existsByCpf(request.cpf())) {
            throw new CpfAlreadyExistsException(request.cpf());
        }
        if (repository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        // 2. Cria usuário no provedor de identidade — senha delegada, nunca persistida no account-service (RNF-04)
        String providerId = identityProvider.createUser(
                request.email(),
                request.password(),
                request.fullName()
        );

        // 3. SAGA: a partir daqui, qualquer falha exige compensação no provedor de identidade
        try {
            Account account = Account.builder()
                    .id(UUID.fromString(providerId))
                    .cpf(request.cpf())
                    .email(request.email())
                    .fullName(request.fullName())
                    .accountType(request.accountType())
                    .build();

            Account saved = repository.save(account);

            // 4. Evento publicado somente após persistência bem-sucedida (RF-03)
            eventPublisher.publish(AccountCreatedEvent.from(saved));

            return new AccountCreatedResponse(saved.getId());

        } catch (Exception e) {
            log.error("Falha ao completar cadastro — revertendo criação no provedor de identidade. providerId={}", providerId, e);
            identityProvider.deleteUser(providerId);
            throw new AccountRegistrationException("Falha no cadastro da conta", e);
        }
    }

}

