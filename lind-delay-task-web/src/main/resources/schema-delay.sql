CREATE TABLE IF NOT EXISTS delay_task (
    id          VARCHAR(64)  NOT NULL PRIMARY KEY,
    biz_type    VARCHAR(64)  NOT NULL,
    biz_id      VARCHAR(128) NOT NULL,
    payload     VARCHAR(2048) NULL,
    delay_ms    BIGINT       NOT NULL,
    execute_at  BIGINT       NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    version     INT          NOT NULL DEFAULT 0,
    created_at  BIGINT       NOT NULL,
    updated_at  BIGINT       NOT NULL,
    INDEX idx_delay_task_status (status),
    INDEX idx_delay_task_execute_at (execute_at),
    INDEX idx_delay_task_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
