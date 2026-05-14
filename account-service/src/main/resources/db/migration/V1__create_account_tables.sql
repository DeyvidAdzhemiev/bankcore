CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    available_balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number),
    CONSTRAINT chk_accounts_type CHECK (type IN ('CHECKING', 'SAVINGS', 'CREDIT')),
    CONSTRAINT chk_accounts_currency CHECK (currency IN ('USD', 'EUR', 'GBP', 'PLN', 'TRY')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_accounts_available_balance_non_negative CHECK (available_balance >= 0),
    CONSTRAINT chk_accounts_available_balance_lte_balance CHECK (available_balance <= balance)
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_accounts_user_id_status ON accounts (user_id, status);
CREATE INDEX idx_accounts_status ON accounts (status);
