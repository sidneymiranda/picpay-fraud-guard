-- ============================================================================
-- Migration: V2__create_account_outbox_event_table.sql
-- Descrição: Cria a tabela account_outbox_event para o padrão Outbox
--
-- Padrão Outbox:
--   Eventos são gravados nesta tabela atomicamente junto com a conta (mesma
--   transação JPA). Um poller externo (OutboxEventPollerScheduler) lê os
--   registros PENDING e os publica no tópico Kafka `account.created`.
--   Isso garante zero perda de eventos mesmo em caso de falha do broker.
--
-- Fluxo:
--   INSERT account + INSERT account_outbox_event  →  COMMIT (tudo ou nada)
--   Poller:  SELECT PENDING → PUBLISH Kafka → UPDATE status = SENT
-- ============================================================================

CREATE TABLE account_outbox_event (
    -- PK técnica (BIGSERIAL — ordem de inserção = ordem de replicação)
    id          BIGSERIAL       NOT NULL,

    -- ID do usuário associado ao evento (referência ao account.external_id)
    user_id     UUID            NOT NULL,

    -- Tópico Kafka de destino (ex: "account.created")
    topic       VARCHAR(100)    NOT NULL,

    -- Payload serializado em JSON — contém AccountCreatedEvent completo
    payload     JSONB           NOT NULL,

    -- Status do envio: PENDING (aguardando replicação) | SENT (replicado com sucesso)
    status      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    -- Momento exato em que o evento foi gerado (gravado via @PrePersist)
    created_at  TIMESTAMP       NOT NULL,

    -- Preenchido pelo poller ao confirmar envio ao broker
    sent_at     TIMESTAMP       NULL,

    -- Constraints
    CONSTRAINT pk_account_outbox_event PRIMARY KEY (id),
    CONSTRAINT fk_outbox_user_id       FOREIGN KEY (user_id) REFERENCES account (external_id),
    CONSTRAINT chk_outbox_status       CHECK (status IN ('PENDING', 'SENT'))
);

-- Índice composto (status, created_at): otimiza a query do poller
--   SELECT * FROM account_outbox_event WHERE status = 'PENDING' ORDER BY created_at ASC
CREATE INDEX idx_outbox_status_created ON account_outbox_event (status, created_at);

-- Índice por topic: para filtros por tipo de evento quando houver múltiplos topics
CREATE INDEX idx_outbox_topic ON account_outbox_event (topic);

-- Índice por user_id: para rastreamento de eventos por usuário (debug/observabilidade)
CREATE INDEX idx_outbox_user_id ON account_outbox_event (user_id);

-- Comentários para documentação
COMMENT ON TABLE  account_outbox_event            IS 'Outbox Pattern — eventos pendentes de publicação no broker Kafka';
COMMENT ON COLUMN account_outbox_event.id         IS 'Chave primária técnica — define ordem FIFO de replicação';
COMMENT ON COLUMN account_outbox_event.user_id    IS 'Referência ao external_id da conta associada ao evento';
COMMENT ON COLUMN account_outbox_event.topic      IS 'Tópico Kafka de destino do evento';
COMMENT ON COLUMN account_outbox_event.payload    IS 'Payload JSON do evento de domínio (AccountCreatedEvent)';
COMMENT ON COLUMN account_outbox_event.status     IS 'PENDING: aguardando envio ao broker | SENT: replicado com sucesso';
COMMENT ON COLUMN account_outbox_event.created_at IS 'Timestamp de criação do evento (momento de inserção)';
COMMENT ON COLUMN account_outbox_event.sent_at    IS 'Timestamp de confirmação de envio ao broker (null = ainda não enviado)';

