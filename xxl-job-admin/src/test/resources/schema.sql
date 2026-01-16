create table if not exists xxl_job_info (
    id                        INT PRIMARY KEY AUTO_INCREMENT,
    job_group                 INT          NOT NULL,
    job_desc                  VARCHAR(255) NOT NULL,
    add_time                  TIMESTAMP            DEFAULT NULL,
    update_time               TIMESTAMP            DEFAULT NULL,
    author                    VARCHAR(64)          DEFAULT NULL,
    alarm_email               VARCHAR(255)         DEFAULT NULL,
    schedule_type             VARCHAR(50)  NOT NULL DEFAULT 'NONE',
    schedule_conf             VARCHAR(128)         DEFAULT NULL,
    misfire_strategy          VARCHAR(50)  NOT NULL DEFAULT 'DO_NOTHING',
    executor_route_strategy   VARCHAR(50)          DEFAULT NULL,
    executor_handler          VARCHAR(255)         DEFAULT NULL,
    executor_param            VARCHAR(512)         DEFAULT NULL,
    executor_block_strategy   VARCHAR(50)          DEFAULT NULL,
    executor_timeout          INT          NOT NULL DEFAULT 0,
    executor_fail_retry_count INT          NOT NULL DEFAULT 0,
    glue_type                 VARCHAR(50)  NOT NULL,
    glue_source               CLOB,
    glue_remark               VARCHAR(128)         DEFAULT NULL,
    glue_updatetime           TIMESTAMP            DEFAULT NULL,
    child_jobid               VARCHAR(255)         DEFAULT NULL,
    trigger_status            TINYINT      NOT NULL DEFAULT 0,
    trigger_last_time         BIGINT       NOT NULL DEFAULT 0,
    trigger_next_time         BIGINT       NOT NULL DEFAULT 0
);

create table if not exists xxl_job_registry
(
    id                        INT PRIMARY KEY AUTO_INCREMENT,
    registry_group varchar(50)  NOT NULL,
    registry_key   varchar(255) NOT NULL,
    registry_value varchar(255) NOT NULL,
    update_time    TIMESTAMP DEFAULT NULL
);

create table if not exists xxl_job_group
(
    id                        INT PRIMARY KEY AUTO_INCREMENT,
    app_name     varchar(64) NOT NULL ,
    title        varchar(12) NOT NULL ,
    address_type INT  NOT NULL DEFAULT '0' ,
    address_list CLOB ,
    update_time  TIMESTAMP             DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS xxl_job_log (
    id                        BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_group                 INT       NOT NULL,
    job_id                    INT       NOT NULL,
    executor_address          VARCHAR(255)        DEFAULT NULL,
    executor_handler          VARCHAR(255)        DEFAULT NULL,
    executor_param            VARCHAR(512)        DEFAULT NULL,
    executor_sharding_param   VARCHAR(20)         DEFAULT NULL,
    executor_fail_retry_count INT       NOT NULL DEFAULT 0,
    trigger_time              TIMESTAMP           DEFAULT NULL,
    trigger_code              INT       NOT NULL,
    trigger_msg               CLOB,
    handle_time               TIMESTAMP           DEFAULT NULL,
    handle_code               INT       NOT NULL,
    handle_msg                CLOB,
    alarm_status              TINYINT   NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS xxl_job_logglue (
  id INT PRIMARY KEY AUTO_INCREMENT,
  job_id int NOT NULL ,
  glue_type varchar(50) DEFAULT NULL ,
  glue_source clob,
  glue_remark varchar(128) NOT NULL,
  add_time TIMESTAMP DEFAULT NULL,
  update_time TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS xxl_job_user (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username varchar(50) NOT NULL,
  password varchar(50) NOT NULL,
  role int NOT NULL,
  permission varchar(255) DEFAULT NULL
);
