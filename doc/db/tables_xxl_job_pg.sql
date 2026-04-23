-- Create table
CREATE TABLE xxl_job_group (
                               id SERIAL NOT NULL PRIMARY KEY,
                               app_name VARCHAR(150) NOT NULL,
                               title VARCHAR(50) NOT NULL,
                               address_type TINYINT NOT NULL DEFAULT 0,
                               address_list TEXT,
                               update_time TIMESTAMP DEFAULT NULL
);

COMMENT ON COLUMN xxl_job_group.app_name IS '执行器AppName';
COMMENT ON COLUMN xxl_job_group.title IS '执行器名称';
COMMENT ON COLUMN xxl_job_group.address_type IS '执行器地址类型：0=自动注册、1=手动录入';
COMMENT ON COLUMN xxl_job_group.address_list IS '执行器地址列表，多地址逗号分隔';

CREATE TABLE xxl_job_info (
                              id SERIAL NOT NULL PRIMARY KEY,
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
                              glue_source TEXT,  -- 使用TEXT类型替代MEDIUMTEXT  
                              glue_remark VARCHAR(128) DEFAULT NULL,
                              glue_updatetime TIMESTAMP DEFAULT NULL,
                              child_jobid VARCHAR(512) DEFAULT NULL,
                              trigger_status SMALLINT NOT NULL DEFAULT 0,  -- 使用SMALLINT替代TINYINT(4)  
                              trigger_last_time BIGINT NOT NULL DEFAULT 0,
                              trigger_next_time BIGINT NOT NULL DEFAULT 0
);

COMMENT ON COLUMN xxl_job_info.job_group IS '执行器主键ID';
COMMENT ON COLUMN xxl_job_info.author IS '作者';
COMMENT ON COLUMN xxl_job_info.alarm_email IS '报警邮件';
COMMENT ON COLUMN xxl_job_info.schedule_type IS '调度类型';
COMMENT ON COLUMN xxl_job_info.schedule_conf IS '调度配置，值含义取决于调度类型';
COMMENT ON COLUMN xxl_job_info.misfire_strategy IS '调度过期策略';
COMMENT ON COLUMN xxl_job_info.executor_route_strategy IS '执行器路由策略';
COMMENT ON COLUMN xxl_job_info.executor_handler IS '执行器任务handler';
COMMENT ON COLUMN xxl_job_info.executor_param IS '执行器任务参数';
COMMENT ON COLUMN xxl_job_info.executor_block_strategy IS '阻塞处理策略';
COMMENT ON COLUMN xxl_job_info.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN xxl_job_info.executor_fail_retry_count IS '失败重试次数';
COMMENT ON COLUMN xxl_job_info.glue_type IS 'GLUE类型';
COMMENT ON COLUMN xxl_job_info.glue_source IS 'GLUE源代码';
COMMENT ON COLUMN xxl_job_info.glue_remark IS 'GLUE备注';
COMMENT ON COLUMN xxl_job_info.glue_updatetime IS 'GLUE更新时间';
COMMENT ON COLUMN xxl_job_info.child_jobid IS '子任务ID，多个逗号分隔';
COMMENT ON COLUMN xxl_job_info.trigger_status IS '调度状态：0-停止，1-运行';
COMMENT ON COLUMN xxl_job_info.trigger_last_time IS '上次调度时间';
COMMENT ON COLUMN xxl_job_info.trigger_next_time IS '下次调度时间';

CREATE TABLE xxl_job_lock (
                              lock_name VARCHAR(50) NOT NULL,
                              PRIMARY KEY (lock_name)
);

COMMENT ON COLUMN xxl_job_lock.lock_name IS '锁名称';

CREATE TABLE xxl_job_log (
                             id BIGSERIAL NOT NULL PRIMARY KEY,
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

COMMENT ON COLUMN xxl_job_log.job_group IS '执行器主键ID';
COMMENT ON COLUMN xxl_job_log.job_id IS '任务，主键ID';
COMMENT ON COLUMN xxl_job_log.executor_address IS '执行器地址，本次执行的地址';
COMMENT ON COLUMN xxl_job_log.executor_handler IS '执行器任务handler';
COMMENT ON COLUMN xxl_job_log.executor_param IS '执行器任务参数';
COMMENT ON COLUMN xxl_job_log.executor_sharding_param IS '执行器任务分片参数，格式如 1/2';
COMMENT ON COLUMN xxl_job_log.executor_fail_retry_count IS '失败重试次数';
COMMENT ON COLUMN xxl_job_log.trigger_time IS '调度-时间';
COMMENT ON COLUMN xxl_job_log.trigger_code IS '调度-结果';
COMMENT ON COLUMN xxl_job_log.trigger_msg IS '调度-日志';
COMMENT ON COLUMN xxl_job_log.handle_time IS '执行-时间';
COMMENT ON COLUMN xxl_job_log.handle_code IS '执行-状态';
COMMENT ON COLUMN xxl_job_log.handle_msg IS '执行-日志';
COMMENT ON COLUMN xxl_job_log.alarm_status IS '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败';
  
-- 创建索引  
CREATE INDEX I_trigger_time ON xxl_job_log (trigger_time);
CREATE INDEX I_handle_code ON xxl_job_log (handle_code);


CREATE TABLE xxl_job_log_report (
                                    id SERIAL NOT NULL PRIMARY KEY,
                                    trigger_day TIMESTAMP DEFAULT NULL,
                                    running_count INT NOT NULL DEFAULT 0,
                                    suc_count INT NOT NULL DEFAULT 0,
                                    fail_count INT NOT NULL DEFAULT 0,
                                    update_time TIMESTAMP DEFAULT NULL
);

COMMENT ON COLUMN xxl_job_log_report.trigger_day IS '调度-时间';
COMMENT ON COLUMN xxl_job_log_report.running_count IS '运行中-日志数量';
COMMENT ON COLUMN xxl_job_log_report.suc_count IS '执行成功-日志数量';
COMMENT ON COLUMN xxl_job_log_report.fail_count IS '执行失败-日志数量';
  
-- 创建唯一索引  
CREATE UNIQUE INDEX i_trigger_day ON xxl_job_log_report (trigger_day);

CREATE TABLE xxl_job_logglue (
                                 id SERIAL NOT NULL PRIMARY KEY,
                                 job_id INT NOT NULL ,
                                 glue_type VARCHAR(50) DEFAULT NULL,
                                 glue_source TEXT,
                                 glue_remark VARCHAR(128) NOT NULL,
                                 add_time TIMESTAMP DEFAULT NULL,
                                 update_time TIMESTAMP DEFAULT NULL
);

COMMENT ON COLUMN xxl_job_logglue.job_id IS '任务，主键ID';
COMMENT ON COLUMN xxl_job_logglue.glue_type IS 'GLUE类型';
COMMENT ON COLUMN xxl_job_logglue.glue_source IS 'GLUE源代码';
COMMENT ON COLUMN xxl_job_logglue.glue_remark IS 'GLUE备注';


CREATE TABLE xxl_job_registry (
                                  id SERIAL PRIMARY KEY,
                                  registry_group VARCHAR(50) NOT NULL,
                                  registry_key VARCHAR(255) NOT NULL,
                                  registry_value VARCHAR(255) NOT NULL,
                                  update_time TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX i_g_k_v ON xxl_job_registry(registry_group, registry_key, registry_value);

CREATE TABLE xxl_job_user (
                              id SERIAL PRIMARY KEY,
                              username VARCHAR(50) NOT NULL,
                              password VARCHAR(50) NOT NULL,
                              role SMALLINT NOT NULL,
                              permission VARCHAR(255),
                              UNIQUE (username)
);

COMMENT ON COLUMN xxl_job_user.username IS '账号';
COMMENT ON COLUMN xxl_job_user.password IS '密码';
COMMENT ON COLUMN xxl_job_user.role IS '角色：0-普通用户、1-管理员';
COMMENT ON COLUMN xxl_job_user.permission IS '权限：执行器ID列表，多个逗号分割';


INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time) VALUES (1, 'framework-task-executor', '示例执行器', 0, NULL, to_date('2018-11-03 22:21:31','yyyy-mm-dd hh24:mi:ss'));
INSERT INTO xxl_job_info(id, job_group, job_desc, add_time, update_time, author, alarm_email, schedule_type, schedule_conf, misfire_strategy, executor_route_strategy, executor_handler, executor_param, executor_block_strategy, executor_timeout, executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime, child_jobid) VALUES (1, 1, '测试任务', to_date('20181103222131','yyyymmddhh24miss'), to_date('20181103222131','yyyymmddhh24miss'), 'XXL', '', 'CRON', '0 0 0 * * ? *', 'DO_NOTHING', 'FIRST', 'demoJobHandler', '', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化', to_date('20181103222131','yyyymmddhh24miss'), '');
INSERT INTO xxl_job_user(id, username, password, role, permission) VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL);
INSERT INTO xxl_job_lock ( lock_name) VALUES ( 'schedule_lock');
commit;
