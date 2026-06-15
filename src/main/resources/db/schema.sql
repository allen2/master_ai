CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(64),
    deleted       INTEGER      NOT NULL DEFAULT 0,
    created_at    TEXT         NOT NULL DEFAULT (datetime('now')),
    updated_at    TEXT         NOT NULL DEFAULT (datetime('now'))
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username);

CREATE TABLE IF NOT EXISTS api_keys (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    provider    VARCHAR(100) NOT NULL UNIQUE,
    key_value   TEXT NOT NULL,
    is_active   INTEGER NOT NULL DEFAULT 1,
    description TEXT,
    last_used   TEXT,
    created_at  TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS hedge_fund_flows (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    nodes       TEXT NOT NULL DEFAULT '[]',
    edges       TEXT NOT NULL DEFAULT '[]',
    viewport    TEXT,
    data        TEXT,
    is_template INTEGER NOT NULL DEFAULT 0,
    tags        TEXT,
    created_at  TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS tool_call_cache (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    tool_name     VARCHAR(200)  NOT NULL,
    params_hash   VARCHAR(64)   NOT NULL,
    result_json   TEXT          NOT NULL,
    ttl_seconds   INTEGER       NOT NULL DEFAULT 3600,
    hit_count     INTEGER       NOT NULL DEFAULT 1,
    created_at    TEXT          NOT NULL DEFAULT (datetime('now')),
    updated_at    TEXT          NOT NULL DEFAULT (datetime('now'))
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_tool_call_cache_lookup
    ON tool_call_cache(tool_name, params_hash);
CREATE INDEX IF NOT EXISTS idx_tool_call_cache_expiry
    ON tool_call_cache(created_at);

-- 用户钱包（每人一行，balance 为当前金币余额）
CREATE TABLE IF NOT EXISTS user_wallets (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL UNIQUE,
    balance     INTEGER NOT NULL DEFAULT 0,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_wallets_user_id ON user_wallets(user_id);

-- 金币流水（type: GRANT=管理员发放, DEDUCT=分析消耗）
CREATE TABLE IF NOT EXISTS coin_transactions (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER      NOT NULL,
    amount        INTEGER      NOT NULL,
    type          VARCHAR(20)  NOT NULL,
    reason        VARCHAR(255),
    balance_after INTEGER      NOT NULL,
    created_at    TEXT         NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_coin_transactions_user_id ON coin_transactions(user_id);

-- 用户分析记录（每次 /hedge-fund/run 调用生成一条）
CREATE TABLE IF NOT EXISTS analysis_runs (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER      NOT NULL,
    tickers           VARCHAR(255) NOT NULL,
    model_name        VARCHAR(100),
    selected_analysts TEXT,
    status            VARCHAR(20)  NOT NULL DEFAULT 'RUNNING',
    analyst_signals   TEXT,
    decisions         TEXT,
    error_message     TEXT,
    created_at        TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at        TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_analysis_runs_user_id ON analysis_runs(user_id);

CREATE TABLE IF NOT EXISTS hedge_fund_flow_runs (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    flow_id           INTEGER NOT NULL,
    status            VARCHAR(50) NOT NULL DEFAULT 'IDLE',
    trading_mode      VARCHAR(50) NOT NULL DEFAULT 'one-time',
    schedule          VARCHAR(50),
    duration          VARCHAR(50),
    request_data      TEXT,
    initial_portfolio TEXT,
    final_portfolio   TEXT,
    results           TEXT,
    error_message     TEXT,
    run_number        INTEGER NOT NULL DEFAULT 1,
    started_at        TEXT,
    completed_at      TEXT,
    created_at        TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at        TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (flow_id) REFERENCES hedge_fund_flows(id)
);

-- 留言板主表（所有登录用户可见，逻辑删除）
CREATE TABLE IF NOT EXISTS message_board (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    content     TEXT    NOT NULL,
    like_count  INTEGER NOT NULL DEFAULT 0,
    deleted     INTEGER NOT NULL DEFAULT 0,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_message_board_created_at ON message_board(created_at);
CREATE INDEX IF NOT EXISTS idx_message_board_user_id ON message_board(user_id);

-- 留言点赞记录（唯一索引防止重复点赞，用于判断"我是否已点赞"）
CREATE TABLE IF NOT EXISTS message_board_likes (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id  INTEGER NOT NULL,
    user_id     INTEGER NOT NULL,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_message_board_likes_msg_user
    ON message_board_likes(message_id, user_id);
