package com.github.sidneymiranda.fraudguard.accountservice.application.gateway;

import com.github.sidneymiranda.fraudguard.accountservice.domain.event.AccountCreatedEvent;

/**
 * Gateway de publicação de eventos — porta driven da camada de aplicação.
 *
 * <p>A implementação concreta será responsável por publicar
 * o evento no barramento de eventos configurado (RF-03).
 */
public interface EventPublisher {

    /**
     * Publica o evento de criação de conta no barramento de eventos.
     * Chamado somente após a persistência bem-sucedida da {@code Account}.
     *
     * @param event evento gerado a partir da entidade já salva
     */
    void publish(AccountCreatedEvent event);
}

