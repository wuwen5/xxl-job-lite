package com.xxl.job.admin.controller;

import com.xxl.job.admin.service.impl.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for JobLogController (/joblog).
 */
public class JobLogControllerTest extends AbstractSpringMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(JobLogControllerTest.class);

    private Cookie cookie;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void prepareData() throws Exception {
        jdbcTemplate.execute("DELETE FROM xxl_job_user WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_log WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_info WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_group WHERE id > 0");

        // admin user
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_user(id, username, password, role, permission) " +
                "VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL)");

        // job group
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time) " +
                "VALUES (1, 'test-executor', 'Test Executor', 0, NULL, NOW())");

        // job info
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_info(id, job_group, job_desc, add_time, update_time, author, schedule_type, " +
                "misfire_strategy, glue_type, trigger_status, trigger_last_time, trigger_next_time) " +
                "VALUES (1, 1, 'Test Job', NOW(), NOW(), 'tester', 'NONE', 'DO_NOTHING', 'BEAN', 0, 0, 0)");

        // login
        MvcResult ret = mockMvc.perform(
                post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456")
        ).andReturn();
        cookie = ret.getResponse().getCookie(LoginService.LOGIN_IDENTITY_KEY);
    }

    // ---------------------- index ----------------------

    /**
     * GET /joblog → returns joblog index view with group list
     */
    @Test
    public void testIndex() throws Exception {
        mockMvc.perform(get("/joblog").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(view().name("joblog/joblog.index"))
                .andExpect(model().attributeExists("JobGroupList"));
    }

    /**
     * GET /joblog?jobId=1 → returns view with jobInfo model attribute
     */
    @Test
    public void testIndexWithJobId() throws Exception {
        mockMvc.perform(get("/joblog").param("jobId", "1").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(view().name("joblog/joblog.index"))
                .andExpect(model().attributeExists("jobInfo"));
    }

    /**
     * GET /joblog?jobId=9999 → job not found → WebExceptionResolver → common.exception view
     */
    @Test
    public void testIndexJobNotFound() throws Exception {
        mockMvc.perform(get("/joblog").param("jobId", "9999").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(view().name("/common/common.exception"))
                .andExpect(model().attributeExists("exceptionMsg"));
    }

    // ---------------------- getJobsByGroup ----------------------

    /**
     * GET /joblog/getJobsByGroup → returns jobs list for the group
     */
    @Test
    public void testGetJobsByGroup() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/joblog/getJobsByGroup")
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
        MvcResult result = mockMvc.perform(
                        post("/joblog/getJobsByGroup")
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
        MvcResult result = mockMvc.perform(
                        post("/joblog/pageList")
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
        MvcResult result = mockMvc.perform(
                        post("/joblog/pageList")
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

    // ---------------------- logDetailPage ----------------------

    /**
     * GET /joblog/logDetailPage?id=1 → returns log detail view
     */
    @Test
    public void testLogDetailPage() throws Exception {
        // insert a log entry
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, trigger_time, trigger_code, handle_code, alarm_status) " +
                "VALUES (1, 1, 1, NOW(), 200, 0, 0)");

        MvcResult result = mockMvc.perform(
                        get("/joblog/logDetailPage")
                                .param("id", "1")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(view().name("joblog/joblog.detail"))
                .andExpect(model().attributeExists("triggerCode", "handleCode", "logId"))
                .andReturn();
        logger.info("testLogDetailPage view: {}", result.getModelAndView() != null ? result.getModelAndView().getViewName() : null);
    }

    /**
     * GET /joblog/logDetailPage?id=9999 → log not found → exception view
     */
    @Test
    public void testLogDetailPageNotFound() throws Exception {
        mockMvc.perform(
                        get("/joblog/logDetailPage")
                                .param("id", "9999")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(view().name("/common/common.exception"))
                .andExpect(model().attributeExists("exceptionMsg"));
    }

    // ---------------------- logDetailCat ----------------------

    /**
     * POST /joblog/logDetailCat with invalid logId → code 500
     */
    @Test
    public void testLogDetailCatInvalidLogId() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/joblog/logDetailCat")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("logId", "9999")
                                .param("fromLineNum", "1")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testLogDetailCatInvalidLogId: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- logKill ----------------------

    /**
     * POST /joblog/logKill → log exists but triggerCode != SUCCESS → code 500
     */
    @Test
    public void testLogKillTriggerNotSuccess() throws Exception {
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, trigger_time, trigger_code, handle_code, alarm_status) " +
                "VALUES (2, 1, 1, NOW(), 500, 0, 0)");  // triggerCode=500 (not SUCCESS)

        MvcResult result = mockMvc.perform(
                        post("/joblog/logKill")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("id", "2")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testLogKillTriggerNotSuccess: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- clearLog ----------------------

    /**
     * POST /joblog/clearLog type=9 → clear all logs → SUCCESS
     */
    @Test
    public void testClearLogAll() throws Exception {
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_log(id, job_group, job_id, trigger_time, trigger_code, handle_code, alarm_status) " +
                "VALUES (3, 1, 1, NOW(), 200, 200, 0)");

        MvcResult result = mockMvc.perform(
                        post("/joblog/clearLog")
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
        MvcResult result = mockMvc.perform(
                        post("/joblog/clearLog")
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
        MvcResult result = mockMvc.perform(
                        post("/joblog/clearLog")
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
}

