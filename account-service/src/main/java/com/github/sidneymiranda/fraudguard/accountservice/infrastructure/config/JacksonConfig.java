package com.github.sidneymiranda.fraudguard.accountservice.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração central do {@link ObjectMapper} da aplicação.
 *
 * <p>Um bean único evita instâncias espalhadas pelo código e garante que
 * todo o sistema serializa/desserializa com as mesmas regras — especialmente
 * {@link java.time.Instant} e outros tipos {@code java.time.*} usados nos
 * eventos de domínio ({@code AccountCreatedEvent}).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}

