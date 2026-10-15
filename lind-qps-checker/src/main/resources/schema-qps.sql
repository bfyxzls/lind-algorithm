CREATE TABLE IF NOT EXISTS qps_job (
    id                        VARCHAR(32)  PRIMARY KEY,
    mode                      VARCHAR(16)  NOT NULL,
    status                    VARCHAR(16)  NOT NULL,
    message                   VARCHAR(2048),
    url                       VARCHAR(2048) NOT NULL,
    method                    VARCHAR(16)  NOT NULL,
    concurrency               INT          NOT NULL,
    duration_seconds          BIGINT       NOT NULL,
    timeout_ms                BIGINT       NOT NULL,
    headers_json              CLOB,
    query_params_json         CLOB,
    form_params_json          CLOB,
    body                      CLOB,
    start_concurrency         INT,
    step                      INT,
    max_concurrency           INT,
    min_success_rate_percent  DOUBLE,
    max_allowed_latency_ms    BIGINT,
    created_at                TIMESTAMP    NOT NULL,
    finished_at               TIMESTAMP,
    report_json               CLOB,
    capacity_json             CLOB
);

CREATE INDEX IF NOT EXISTS idx_qps_job_created ON qps_job (created_at DESC);

-- 测试用例文件夹
CREATE TABLE IF NOT EXISTS qps_folder (
    id          VARCHAR(32)  PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    parent_id   VARCHAR(32),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_qps_folder_parent ON qps_folder (parent_id);

-- 可复用的压测用例（按文件夹管理）
CREATE TABLE IF NOT EXISTS qps_case (
    id                        VARCHAR(32)   PRIMARY KEY,
    folder_id                 VARCHAR(32)   NOT NULL,
    name                      VARCHAR(256)  NOT NULL,
    description               VARCHAR(1024),
    url                       VARCHAR(2048) NOT NULL,
    method                    VARCHAR(16)   NOT NULL,
    mode                      VARCHAR(16)   NOT NULL,
    concurrency               INT           NOT NULL,
    duration_seconds          BIGINT        NOT NULL,
    timeout_ms                BIGINT        NOT NULL,
    headers_text              CLOB,
    query_params_text         CLOB,
    form_params_text          CLOB,
    body                      CLOB,
    start_concurrency         INT,
    step                      INT,
    max_concurrency           INT,
    min_success_rate_percent  DOUBLE,
    max_allowed_latency_ms    BIGINT,
    created_at                TIMESTAMP     NOT NULL,
    updated_at                TIMESTAMP     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_qps_case_folder ON qps_case (folder_id);
CREATE INDEX IF NOT EXISTS idx_qps_case_updated ON qps_case (updated_at DESC);
