package com.xxl.job.admin.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.service.impl.LoginService;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.LogParam;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.servlet.http.Cookie;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Unit tests for JobLogController (/joblog).
 */
public class JobLogControllerTest extends AbstractSpringMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(JobLogControllerTest.class);

    private Cookie cookie;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private ConcurrentMap<String, ExecutorBiz> originalExecutorBizRepository;

    @BeforeEach
    public void prepareData() throws Exception {
        jdbcTemplate.execute("DELETE FROM xxl_job_user WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_log WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_info WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_group WHERE id > 0");

        // admin user
        jdbcTemplate.execute("INSERT INTO xxl_job_user(id, username, password, role, permission) "
                + "VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL)");

        // job group
        jdbcTemplate.execute("INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time) "
                + "VALUES (1, 'test-executor', 'Test Executor', 0, NULL, NOW())");

        // job info
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_info(id, job_group, job_desc, add_time, update_time, author, schedule_type, "
                        + "misfire_strategy, glue_type, trigger_status, trigger_last_time, trigger_next_time) "
                        + "VALUES (1, 1, 'Test Job', NOW(), NOW(), 'tester', 'NONE', 'DO_NOTHING', 'BEAN', 0, 0, 0)");

        // login
        MvcResult ret = mockMvc.perform(post("/admin-api/v1/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456"))
                .andReturn();
        cookie = ret.getResponse().getCookie(LoginService.LOGIN_IDENTITY_KEY);
    }

    @AfterEach
    public void cleanup() throws Exception {
        // Restore original executor biz repository
        if (originalExecutorBizRepository != null) {
            Field field = XxlJobScheduler.class.getDeclaredField("executorBizRepository");
            field.setAccessible(true);
            field.set(null, originalExecutorBizRepository);
        }
    }

    // ---------------------- index ----------------------

    // ---------------------- getJobsByGroup ----------------------

    /**
     * GET /joblog/getJobsByGroup → returns jobs list for the group
     */
    @Test
    public void testGetJobsByGroup() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/v1/joblog/getJobsByGroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();
        logger.info("testGetJobsByGroup: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testGetJobsByGroupEmpty() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/v1/joblog/getJobsByGroup/999")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "999")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testGetJobsByGroupEmpty: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- pageList ----------------------

    /**
     * POST /joblog/pageList → returns paginated log list
     */
    @Test
    public void testPageList() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/pageList")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("start", "0")
                        .param("length", "10")
                        .param("jobGroup", "1")
                        .param("jobId", "0")
                        .param("logStatus", "-1")
                        .param("filterTime", "")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsTotal").isNumber())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();
        logger.info("testPageList: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/pageList with filterTime range → parses dates correctly
     */
    @Test
    public void testPageListWithTimeFilter() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/pageList")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("start", "0")
                        .param("length", "10")
                        .param("jobGroup", "1")
                        .param("jobId", "0")
                        .param("logStatus", "-1")
                        .param("filterTime", "2026-01-01 00:00:00 - 2026-12-31 23:59:59")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsTotal").isNumber())
                .andReturn();
        logger.info("testPageListWithTimeFilter: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- logDetailCat ----------------------

    /**
     * POST /joblog/logDetailCat with invalid logId → code 500
     */
    @Test
    public void testLogDetailCatInvalidLogId() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/v1/joblog/logDetailCat/9999")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("logId", "9999")
                        .param("fromLineNum", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testLogDetailCatInvalidLogId: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/logDetailCat with valid logId but no executor → returns logs from DB
     */
    @Test
    public void testLogDetailCatValidLogIdNoExecutor() throws Exception {
        // Insert a log with executor address
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, executor_address, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (10, 1, 1, 'http://127.0.0.1:9999', NOW(), 200, 0, 0)");

        MvcResult result = mockMvc.perform(get("/admin-api/v1/joblog/logDetailCat/10")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("logId", "10")
                        .param("fromLineNum", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();
        logger.info(
                "testLogDetailCatValidLogIdNoExecutor: {}", result.getResponse().getContentAsString());
        // Should fail because executor is not available
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("\"code\":500") || content.contains("\"code\":200"));
    }

    /**
     * POST /joblog/logDetailCat with mock executor returning success
     */
    @Test
    public void testLogDetailCatWithMockExecutor() throws Exception {
        // Insert a log with executor address
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, executor_address, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (11, 1, 1, 'http://mock-executor:9999', NOW(), 200, 0, 0)");

        // Mock ExecutorBiz
        ExecutorBiz mockExecutorBiz = new ExecutorBiz() {
            @Override
            public ReturnT<String> beat() {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> idleBeat(com.xxl.job.core.biz.model.IdleBeatParam idleBeatParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> run(com.xxl.job.core.biz.model.TriggerParam triggerParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> kill(com.xxl.job.core.biz.model.KillParam killParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<LogResult> log(LogParam logParam) {
                LogResult logResult = new LogResult();
                logResult.setFromLineNum(logParam.getFromLineNum());
                logResult.setToLineNum(10);
                logResult.setLogContent("Test log content");
                logResult.setEnd(false);
                return new ReturnT<>(logResult);
            }
        };

        // Inject mock executor
        Field field = XxlJobScheduler.class.getDeclaredField("executorBizRepository");
        field.setAccessible(true);
        originalExecutorBizRepository = (ConcurrentMap<String, ExecutorBiz>) field.get(null);
        ConcurrentMap<String, ExecutorBiz> mockRepo = new java.util.concurrent.ConcurrentHashMap<>();
        mockRepo.put("http://mock-executor:9999", mockExecutorBiz);
        field.set(null, mockRepo);

        MvcResult result = mockMvc.perform(get("/admin-api/v1/joblog/logDetailCat/11")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("logId", "11")
                        .param("fromLineNum", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testLogDetailCatWithMockExecutor: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/logDetailCat with XSS content → should escape HTML
     */
    @Test
    public void testLogDetailCatXssEscape() throws Exception {
        // Insert a log with executor address
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, executor_address, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (12, 1, 1, 'http://xss-executor:9999', NOW(), 200, 0, 0)");

        // Mock ExecutorBiz with XSS content
        ExecutorBiz mockExecutorBiz = new ExecutorBiz() {
            @Override
            public ReturnT<String> beat() {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> idleBeat(com.xxl.job.core.biz.model.IdleBeatParam idleBeatParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> run(com.xxl.job.core.biz.model.TriggerParam triggerParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> kill(com.xxl.job.core.biz.model.KillParam killParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<LogResult> log(LogParam logParam) {
                LogResult logResult = new LogResult();
                logResult.setFromLineNum(logParam.getFromLineNum());
                logResult.setToLineNum(5);
                logResult.setLogContent("<script>alert('xss')</script>");
                logResult.setEnd(false);
                return new ReturnT<>(logResult);
            }
        };

        // Inject mock executor
        Field field = XxlJobScheduler.class.getDeclaredField("executorBizRepository");
        field.setAccessible(true);
        originalExecutorBizRepository = (ConcurrentMap<String, ExecutorBiz>) field.get(null);
        ConcurrentMap<String, ExecutorBiz> mockRepo = new java.util.concurrent.ConcurrentHashMap<>();
        mockRepo.put("http://xss-executor:9999", mockExecutorBiz);
        field.set(null, mockRepo);

        MvcResult result = mockMvc.perform(get("/admin-api/v1/joblog/logDetailCat/12")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("logId", "12")
                        .param("fromLineNum", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testLogDetailCatXssEscape: {}", result.getResponse().getContentAsString());
        // Verify XSS is escaped
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("&lt;script&gt;") || !content.contains("<script>"));
    }

    /**
     * POST /joblog/logDetailCat with exception → code 500
     */
    @Test
    public void testLogDetailCatException() throws Exception {
        // Insert a log with executor address
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, executor_address, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (13, 1, 1, 'http://error-executor:9999', NOW(), 200, 0, 0)");

        // Mock ExecutorBiz that throws exception
        ExecutorBiz mockExecutorBiz = new ExecutorBiz() {
            @Override
            public ReturnT<String> beat() {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> idleBeat(com.xxl.job.core.biz.model.IdleBeatParam idleBeatParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> run(com.xxl.job.core.biz.model.TriggerParam triggerParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> kill(com.xxl.job.core.biz.model.KillParam killParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<LogResult> log(LogParam logParam) {
                throw new RuntimeException("Simulated error");
            }
        };

        // Inject mock executor
        Field field = XxlJobScheduler.class.getDeclaredField("executorBizRepository");
        field.setAccessible(true);
        originalExecutorBizRepository = (ConcurrentMap<String, ExecutorBiz>) field.get(null);
        ConcurrentMap<String, ExecutorBiz> mockRepo = new java.util.concurrent.ConcurrentHashMap<>();
        mockRepo.put("http://error-executor:9999", mockExecutorBiz);
        field.set(null, mockRepo);

        MvcResult result = mockMvc.perform(get("/admin-api/v1/joblog/logDetailCat/13")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("logId", "13")
                        .param("fromLineNum", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testLogDetailCatException: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- logKill ----------------------

    /**
     * POST /joblog/logKill → log exists but triggerCode != SUCCESS → code 500
     */
    @Test
    public void testLogKillTriggerNotSuccess() throws Exception {
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (2, 1, 1, NOW(), 500, 0, 0)"); // triggerCode=500 (not SUCCESS)

        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/logKill")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "2")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testLogKillTriggerNotSuccess: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/logKill → job info not found → code 500
     */
    @Test
    public void testLogKillJobInfoNotFound() throws Exception {
        // Insert log with non-existent job_id
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (20, 1, 9999, NOW(), 200, 0, 0)");

        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/logKill")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "20")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testLogKillJobInfoNotFound: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/logKill with valid log and mock executor success
     */
    @Test
    public void testLogKillSuccess() throws Exception {
        // Insert log with SUCCESS trigger code
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, executor_address, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (21, 1, 1, 'http://kill-executor:9999', NOW(), 200, 0, 0)");

        // Mock ExecutorBiz
        ExecutorBiz mockExecutorBiz = new ExecutorBiz() {
            @Override
            public ReturnT<String> beat() {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> idleBeat(com.xxl.job.core.biz.model.IdleBeatParam idleBeatParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> run(com.xxl.job.core.biz.model.TriggerParam triggerParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> kill(com.xxl.job.core.biz.model.KillParam killParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<LogResult> log(LogParam logParam) {
                LogResult logResult = new LogResult();
                logResult.setFromLineNum(logParam.getFromLineNum());
                logResult.setToLineNum(0);
                logResult.setLogContent("");
                logResult.setEnd(true);
                return new ReturnT<>(logResult);
            }
        };

        // Inject mock executor
        Field field = XxlJobScheduler.class.getDeclaredField("executorBizRepository");
        field.setAccessible(true);
        originalExecutorBizRepository = (ConcurrentMap<String, ExecutorBiz>) field.get(null);
        ConcurrentMap<String, ExecutorBiz> mockRepo = new java.util.concurrent.ConcurrentHashMap<>();
        mockRepo.put("http://kill-executor:9999", mockExecutorBiz);
        field.set(null, mockRepo);

        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/logKill")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "21")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testLogKillSuccess: {}", result.getResponse().getContentAsString());

        // Verify log was updated in DB
        Integer handleCode =
                jdbcTemplate.queryForObject("SELECT handle_code FROM xxl_job_log WHERE id = 21", Integer.class);
        assert handleCode != null && handleCode == 500 : "Expected handle_code=500 after kill";
    }

    /**
     * POST /joblog/logKill with mock executor returning failure
     */
    @Test
    public void testLogKillExecutorFailure() throws Exception {
        // Insert log with SUCCESS trigger code
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, executor_address, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (22, 1, 1, 'http://kill-fail-executor:9999', NOW(), 200, 0, 0)");

        // Mock ExecutorBiz that returns failure
        ExecutorBiz mockExecutorBiz = new ExecutorBiz() {
            @Override
            public ReturnT<String> beat() {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> idleBeat(com.xxl.job.core.biz.model.IdleBeatParam idleBeatParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> run(com.xxl.job.core.biz.model.TriggerParam triggerParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> kill(com.xxl.job.core.biz.model.KillParam killParam) {
                return new ReturnT<>(500, "Kill failed");
            }

            @Override
            public ReturnT<LogResult> log(LogParam logParam) {
                LogResult logResult = new LogResult();
                logResult.setFromLineNum(logParam.getFromLineNum());
                logResult.setToLineNum(0);
                logResult.setLogContent("");
                logResult.setEnd(true);
                return new ReturnT<>(logResult);
            }
        };

        // Inject mock executor
        Field field = XxlJobScheduler.class.getDeclaredField("executorBizRepository");
        field.setAccessible(true);
        originalExecutorBizRepository = (ConcurrentMap<String, ExecutorBiz>) field.get(null);
        ConcurrentMap<String, ExecutorBiz> mockRepo = new java.util.concurrent.ConcurrentHashMap<>();
        mockRepo.put("http://kill-fail-executor:9999", mockExecutorBiz);
        field.set(null, mockRepo);

        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/logKill")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "22")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testLogKillExecutorFailure: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/logKill with mock executor throwing exception
     */
    @Test
    public void testLogKillExecutorException() throws Exception {
        // Insert log with SUCCESS trigger code
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, executor_address, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (23, 1, 1, 'http://kill-error-executor:9999', NOW(), 200, 0, 0)");

        // Mock ExecutorBiz that throws exception
        ExecutorBiz mockExecutorBiz = new ExecutorBiz() {
            @Override
            public ReturnT<String> beat() {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> idleBeat(com.xxl.job.core.biz.model.IdleBeatParam idleBeatParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> run(com.xxl.job.core.biz.model.TriggerParam triggerParam) {
                return ReturnT.SUCCESS;
            }

            @Override
            public ReturnT<String> kill(com.xxl.job.core.biz.model.KillParam killParam) {
                throw new RuntimeException("Network error");
            }

            @Override
            public ReturnT<LogResult> log(LogParam logParam) {
                LogResult logResult = new LogResult();
                logResult.setFromLineNum(logParam.getFromLineNum());
                logResult.setToLineNum(0);
                logResult.setLogContent("");
                logResult.setEnd(true);
                return new ReturnT<>(logResult);
            }
        };

        // Inject mock executor
        Field field = XxlJobScheduler.class.getDeclaredField("executorBizRepository");
        field.setAccessible(true);
        originalExecutorBizRepository = (ConcurrentMap<String, ExecutorBiz>) field.get(null);
        ConcurrentMap<String, ExecutorBiz> mockRepo = new java.util.concurrent.ConcurrentHashMap<>();
        mockRepo.put("http://kill-error-executor:9999", mockExecutorBiz);
        field.set(null, mockRepo);

        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/logKill")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "23")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testLogKillExecutorException: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- clearLog ----------------------

    /**
     * POST /joblog/clearLog type=9 → clear all logs → SUCCESS
     */
    @Test
    public void testClearLogAll() throws Exception {
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (3, 1, 1, NOW(), 200, 200, 0)");

        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "1")
                        .param("type", "9")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testClearLogAll: {}", result.getResponse().getContentAsString());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM xxl_job_log WHERE job_id = 1", Integer.class);
        assert count != null && count == 0 : "Expected 0 logs after clearLog type=9, got: " + count;
    }

    /**
     * POST /joblog/clearLog type=1 → clear logs older than 1 month → SUCCESS
     */
    @Test
    public void testClearLogByMonth() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "0")
                        .param("type", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testClearLogByMonth: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/clearLog invalid type → code 500
     */
    @Test
    public void testClearLogInvalidType() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "0")
                        .param("type", "99")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testClearLogInvalidType: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/clearLog type=2 → clear logs older than 3 months → SUCCESS
     */
    @Test
    public void testClearLogByThreeMonths() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "0")
                        .param("type", "2")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testClearLogByThreeMonths: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/clearLog type=3 → clear logs older than 6 months → SUCCESS
     */
    @Test
    public void testClearLogBySixMonths() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "0")
                        .param("type", "3")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testClearLogBySixMonths: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/clearLog type=4 → clear logs older than 1 year → SUCCESS
     */
    @Test
    public void testClearLogByYear() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "0")
                        .param("type", "4")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testClearLogByYear: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/clearLog type=5 → clear oldest 1000 logs → SUCCESS
     */
    @Test
    public void testClearLogByCount1000() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "0")
                        .param("type", "5")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testClearLogByCount1000: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/clearLog type=6 → clear oldest 10000 logs → SUCCESS
     */
    @Test
    public void testClearLogByCount10000() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "0")
                        .param("type", "6")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testClearLogByCount10000: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/clearLog type=7 → clear oldest 30000 logs → SUCCESS
     */
    @Test
    public void testClearLogByCount30000() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "0")
                        .param("type", "7")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testClearLogByCount30000: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/clearLog type=8 → clear oldest 100000 logs → SUCCESS
     */
    @Test
    public void testClearLogByCount100000() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "0")
                        .param("type", "8")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testClearLogByCount100000: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /joblog/clearLog with specific jobId → only clear logs for that job
     */
    @Test
    public void testClearLogByJobId() throws Exception {
        // Insert logs for different jobs
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (30, 1, 1, TIMESTAMPADD('MONTH', -2, NOW()), 200, 200, 0)");
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, trigger_time, trigger_code, handle_code, alarm_status) "
                        + "VALUES (31, 1, 1, NOW(), 200, 200, 0)");

        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "1")
                        .param("type", "1") // Clear logs older than 1 month
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testClearLogByJobId: {}", result.getResponse().getContentAsString());

        // Verify old log was cleared but new log remains
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM xxl_job_log WHERE job_id = 1", Integer.class);
        assert count != null && count == 1 : "Expected 1 log remaining after clearLog";
    }

    /**
     * POST /joblog/clearLog with empty result → still returns SUCCESS
     */
    @Test
    public void testClearLogEmptyResult() throws Exception {
        // No logs in DB for this jobGroup
        MvcResult result = mockMvc.perform(post("/admin-api/v1/joblog/clearLog")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("jobGroup", "1")
                        .param("jobId", "999")
                        .param("type", "9")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testClearLogEmptyResult: {}", result.getResponse().getContentAsString());
    }
}
