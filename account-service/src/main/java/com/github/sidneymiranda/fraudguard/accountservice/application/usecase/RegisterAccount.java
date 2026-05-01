package com.github.sidneymiranda.fraudguard.accountservice.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sidneymiranda.fraudguard.accountservice.application.dto.AccountCreatedResponse;
import com.github.sidneymiranda.fraudguard.accountservice.application.dto.RegisterAccountRequest;
import com.github.sidneymiranda.fraudguard.accountservice.application.exception.AccountRegistrationException;
import com.github.sidneymiranda.fraudguard.accountservice.application.gateway.IdentityProvider;
import com.github.sidneymiranda.fraudguard.accountservice.domain.Account;
import com.github.sidneymiranda.fraudguard.accountservice.domain.event.AccountCreatedEvent;
import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.CpfAlreadyExistsException;
import com.github.sidneymiranda.fraudguard.accountservice.domain.exception.EmailAlreadyExistsException;
import com.github.sidneymiranda.fraudguard.accountservice.domain.repository.AccountOutboxRepository;
import com.github.sidneymiranda.fraudguard.accountservice.domain.repository.AccountRepository;
import com.github.sidneymiranda.fraudguard.accountservice.infrastructure.persistence.jpa.entity.AccountOutboxEventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: registrar uma nova conta de usuário (RF-01, RF-03).
 *
 * <p>Orquestração (etapas 3 e 4 executam atomicamente):
 * <ol>
 *   <li>Valida unicidade de CPF e e-mail (RF-01) — antes de qualquer efeito colateral externo</li>
 *   <li>Cria usuário no provedor de identidade — senha delegada, nunca armazenada no account-service (RNF-04)</li>
 *   <li>Persiste {@link Account} no repositório — sem senha</li>
 *   <li>Persiste {@link AccountCreatedEvent} no repositório de outbox (Outbox Pattern — RF-03) — mesma unidade de trabalho da etapa 3</li>
 * </ol>
 *
 * <p><b>Garantia de atomicidade:</b> etapas 3 e 4 executam dentro de uma única unidade de trabalho — tudo ou nada.
 * Se a persistência da conta ou do evento no outbox falhar, ambas são revertidas e a compensação
 * no provedor de identidade é disparada manualmente.
 *
 * <p><b>Outbox Pattern:</b> o evento {@code AccountCreated} não é publicado diretamente no barramento de eventos.
 * Ele é registrado de forma durável no repositório de outbox com status {@code PENDING}.
 * Um componente externo (a implementar) lerá periodicamente esses registros e os publicará
 * no barramento, garantindo zero perda de eventos mesmo em caso de indisponibilidade do barramento.
 *
 * <p><b>SAGA com compensação:</b> se qualquer etapa após a criação no provedor de identidade
 * falhar, o usuário é removido via {@code identityProvider.deleteUser} para garantir
 * consistência distribuída. A exceção original é encapsulada em {@link AccountRegistrationException}.
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RegisterAccount {

    private static final Logger log = LoggerFactory.getLogger(RegisterAccount.class);
    private static final String ACCOUNT_CREATED_TOPIC = "account.created";

    private final AccountRepository repository;
    private final AccountOutboxRepository outboxRepository;
    private final IdentityProvider identityProvider;
    private final ObjectMapper objectMapper;

    public RegisterAccount(AccountRepository repository,
                           AccountOutboxRepository outboxRepository,
                           IdentityProvider identityProvider,
                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.identityProvider = identityProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra uma nova conta.
     *
     * @param request dados de registro
     * @return response com o ID da conta criada
     * @throws CpfAlreadyExistsException    se o CPF já estiver cadastrado (RF-01)
     * @throws EmailAlreadyExistsException  se o e-mail já estiver cadastrado (RF-01)
     * @throws AccountRegistrationException se o processo falhar após a criação no provedor de identidade
     */
    public AccountCreatedResponse register(RegisterAccountRequest request) {

        // 1. Validações de unicidade — fail-fast antes de qualquer efeito colateral externo
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

        // 3–4. SAGA: a partir daqui, qualquer falha exige compensação no provedor de identidade.
        //       Ambas as operações (save account + save outbox) executam na mesma transação JPA.
        try {
            Account account = Account.builder()
                    .id(UUID.fromString(providerId))
                    .cpf(request.cpf())
                    .email(request.email())
                    .fullName(request.fullName())
                    .accountType(request.accountType())
                    .build();

            // 3. Persiste conta
            Account saved = repository.save(account);

            // 4. Persiste evento no outbox — mesma transação (tudo ou nada com a conta)
            this.persistToOutbox(AccountCreatedEvent.from(saved));

            log.info("Conta registrada com sucesso. accountId={}", saved.getId());
            return new AccountCreatedResponse(saved.getId());

        } catch (Exception e) {
            log.error("Falha ao completar cadastro — revertendo criação no provedor de identidade. providerId={}", providerId, e);
            compensate(providerId);
            throw new AccountRegistrationException("Falha no cadastro da conta", e);
        }
    }

    // ─── métodos privados ─────────────────────────────────────────────────────

    /**
     * Persiste o {@link AccountCreatedEvent} no repositório de outbox.
     * Executa dentro da mesma unidade de trabalho que persiste a conta — revertida automaticamente em falha.
     *
     * @throws RuntimeException se a serialização do evento falhar,
     *                          garantindo que a unidade de trabalho seja revertida corretamente
     */
    private void persistToOutbox(AccountCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            AccountOutboxEventEntity outboxEvent = new AccountOutboxEventEntity();
            outboxEvent.setUserId(event.userId());
            outboxEvent.setTopic(ACCOUNT_CREATED_TOPIC);
            outboxEvent.setPayload(payload);
            // status PENDING e createdAt são definidos via @PrePersist

            outboxRepository.save(outboxEvent);
            log.debug("Evento persistido no outbox. topic={}, userId={}", ACCOUNT_CREATED_TOPIC, event.userId());

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Falha ao serializar evento para o outbox", e);
        }
    }

    /**
     * Compensação SAGA: remove o usuário do provedor de identidade.
     * Chamado exclusivamente no bloco {@code catch} do fluxo principal — após {@code createUser()} ter sucedido.
     *
     * <p>Falha nesta etapa é logada como CRÍTICA (orfão no provider) mas não suprime a exceção original.
     * Futuramente: implementar retry com exponential back-off + fila de compensação.
     */
    private void compensate(String providerId) {
        try {
            identityProvider.deleteUser(providerId);
            log.info("Compensação executada com sucesso. providerId={}", providerId);
        } catch (Exception ex) {
            log.error("FALHA CRÍTICA NA COMPENSAÇÃO — usuário órfão no provider! providerId={}", providerId, ex);
            // Não re-lança: a exceção original já será encapsulada em AccountRegistrationException
        }
    }
}

