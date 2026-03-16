CREATE TABLE IF NOT EXISTS user_account (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_account_username ON user_account (username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_account_email ON user_account (email);

CREATE TABLE IF NOT EXISTS cart_item (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    checked BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_cart_item_user_product ON cart_item (user_id, product_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_user_id ON cart_item (user_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_product_id ON cart_item (product_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_user_checked ON cart_item (user_id, checked);

CREATE TABLE IF NOT EXISTS order_info (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    items_json JSONB NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    pay_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_order_info_order_no ON order_info (order_no);
CREATE INDEX IF NOT EXISTS idx_order_info_user_id ON order_info (user_id);
CREATE INDEX IF NOT EXISTS idx_order_info_user_created_at ON order_info (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_info_status ON order_info (status);
CREATE INDEX IF NOT EXISTS idx_order_info_pay_status ON order_info (pay_status);

CREATE TABLE IF NOT EXISTS payment_record (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL REFERENCES order_info (order_no) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    pay_amount NUMERIC(10, 2) NOT NULL,
    pay_status VARCHAR(32) NOT NULL,
    alipay_trade_no VARCHAR(128),
    callback_content TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_record_order_no ON payment_record (order_no);
CREATE INDEX IF NOT EXISTS idx_payment_record_user_id ON payment_record (user_id);
CREATE INDEX IF NOT EXISTS idx_payment_record_alipay_trade_no ON payment_record (alipay_trade_no);

CREATE TABLE IF NOT EXISTS chat_session_context (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    session_id VARCHAR(64) NOT NULL,
    last_intent VARCHAR(64),
    last_user_message TEXT,
    last_product_ids_json JSONB,
    last_order_no VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_chat_session_context_session_id ON chat_session_context (session_id);
CREATE INDEX IF NOT EXISTS idx_chat_session_context_user_id ON chat_session_context (user_id);
CREATE INDEX IF NOT EXISTS idx_chat_session_context_user_updated_at ON chat_session_context (user_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS chat_message_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    session_id VARCHAR(64) NOT NULL,
    message_role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_message_history_session_created_at
    ON chat_message_history (session_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_message_history_user_created_at
    ON chat_message_history (user_id, created_at DESC);
