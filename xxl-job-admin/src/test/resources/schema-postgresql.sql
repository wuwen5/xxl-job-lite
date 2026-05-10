CREATE TABLE IF NOT EXISTS xxl_job_group (
    id SERIAL PRIMARY KEY,
    app_name VARCHAR(150) NOT NULL,
    title VARCHAR(50) NOT NULL,
    address_type SMALLINT NOT NULL DEFAULT 0,
    address_list TEXT,
    update_time TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS xxl_job_info (
    id SERIAL PRIMARY KEY,
    job_group INT NOT NULL,
    job_desc VARCHAR(500) NOT NULL,
    add_time TIMESTAMP DEFAULT NULL,
    update_time TIMESTAMP DEFAULT NULL,
    author VARCHAR(64) DEFAULT NULL,
    alarm_email VARCHAR(255) DEFAULT NULL,
    schedule_type VARCHAR(50) NOT NULL DEFAULT 'NONE',
    schedule_conf VARCHAR(128) DEFAULT NULL,
    misfire_strategy VARCHAR(50) NOT NULL DEFAULT 'DO_NOTHING',
    executor_route_strategy VARCHAR(150) DEFAULT NULL,
    executor_handler VARCHAR(512) DEFAULT NULL,
    executor_param VARCHAR(1024) DEFAULT NULL,
    executor_block_strategy VARCHAR(128) DEFAULT NULL,
    executor_timeout INT NOT NULL DEFAULT 0,
    executor_fail_retry_count INT NOT NULL DEFAULT 0,
    glue_type VARCHAR(50) NOT NULL,
    glue_source TEXT,
    glue_remark VARCHAR(128) DEFAULT NULL,
    glue_updatetime TIMESTAMP DEFAULT NULL,
    child_jobid VARCHAR(512) DEFAULT NULL,
    trigger_status SMALLINT NOT NULL DEFAULT 0,
    trigger_last_time BIGINT NOT NULL DEFAULT 0,
    trigger_next_time BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS xxl_job_log (
    id BIGSERIAL PRIMARY KEY,
    job_group INT NOT NULL,
    job_id INT NOT NULL,
    executor_address VARCHAR(255) DEFAULT NULL,
    executor_handler VARCHAR(255) DEFAULT NULL,
    executor_param VARCHAR(512) DEFAULT NULL,
    executor_sharding_param VARCHAR(20) DEFAULT NULL,
    executor_fail_retry_count INT NOT NULL DEFAULT 0,
    trigger_time TIMESTAMP DEFAULT NULL,
    trigger_code INT NOT NULL,
    trigger_msg TEXT,
    handle_time TIMESTAMP DEFAULT NULL,
    handle_code INT NOT NULL,
    handle_msg TEXT,
    alarm_status SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS i_trigger_time ON xxl_job_log (trigger_time);
CREATE INDEX IF NOT EXISTS i_handle_code ON xxl_job_log (handle_code);

CREATE TABLE IF NOT EXISTS xxl_job_registry (
    id SERIAL PRIMARY KEY,
    registry_group VARCHAR(50) NOT NULL,
    registry_key VARCHAR(255) NOT NULL,
    registry_value VARCHAR(255) NOT NULL,
    update_time TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS i_g_k_v ON xxl_job_registry(registry_group, registry_key, registry_value);

CREATE TABLE IF NOT EXISTS xxl_job_lock (
    lock_name VARCHAR(50) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS xxl_job_log_report (
    id SERIAL PRIMARY KEY,
    trigger_day TIMESTAMP DEFAULT NULL,
    running_count INT NOT NULL DEFAULT 0,
    suc_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    update_time TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS xxl_job_logglue (
    id SERIAL PRIMARY KEY,
    job_id INT NOT NULL,
    glue_type VARCHAR(50) DEFAULT NULL,
    glue_source TEXT,
    glue_remark VARCHAR(128) NOT NULL,
    add_time TIMESTAMP DEFAULT NULL,
    update_time TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS xxl_job_user (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    role SMALLINT NOT NULL,
    permission VARCHAR(255)
);
