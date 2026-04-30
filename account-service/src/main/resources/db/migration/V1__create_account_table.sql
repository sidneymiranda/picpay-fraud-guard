-- ============================================================================
-- Migration: V1__create_account_table.sql
-- Descrição: Cria a tabela account com todas as constraints e índices
-- ============================================================================

-- Cria a tabela account
CREATE TABLE account (
    -- PK técnica (BIGSERIAL gera automaticamente sequência no PostgreSQL)
    id            BIGSERIAL       NOT NULL,

    -- ID de negócio (UUID gerado pelo provedor de identidade)
    external_id   UUID            NOT NULL,

    -- Dados do titular
    full_name     VARCHAR(255)    NOT NULL,
    cpf           VARCHAR(11)     NOT NULL,
    email         VARCHAR(255)    NOT NULL,

    -- Tipo de conta (ENUM armazenado como VARCHAR)
    account_type  VARCHAR(20)     NOT NULL,

    -- Auditoria (timestamps)
    created_at    TIMESTAMP       NOT NULL,
    updated_at    TIMESTAMP       NOT NULL,

    -- Constraints
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT uq_account_external_id UNIQUE (external_id),
    CONSTRAINT uq_account_cpf UNIQUE (cpf),
    CONSTRAINT uq_account_email UNIQUE (email)
);

-- Índices para otimizar consultas
CREATE INDEX idx_account_cpf ON account (cpf);
CREATE INDEX idx_account_email ON account (email);
CREATE INDEX idx_account_external_id ON account (external_id);

-- Comentários para documentação
COMMENT ON TABLE account IS 'Tabela de contas de usuários do sistema FraudGuard';
COMMENT ON COLUMN account.id IS 'Chave primária técnica (nunca exposta via API)';
COMMENT ON COLUMN account.external_id IS 'ID de negócio gerado pelo provedor de identidade';
COMMENT ON COLUMN account.full_name IS 'Nome completo do titular (aceita homônimos)';
COMMENT ON COLUMN account.cpf IS 'CPF do titular (único e imutável)';
COMMENT ON COLUMN account.email IS 'E-mail do titular (único)';
COMMENT ON COLUMN account.account_type IS 'Tipo de conta: COMMON ou MERCHANT';
COMMENT ON COLUMN account.created_at IS 'Data/hora de criação do registro';
COMMENT ON COLUMN account.updated_at IS 'Data/hora da última atualização';

