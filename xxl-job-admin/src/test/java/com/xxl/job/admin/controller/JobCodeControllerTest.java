package com.xxl.job.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.xxl.job.admin.service.impl.LoginService;
import com.xxl.job.core.glue.GlueTypeEnum;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Unit tests for JobCodeController (/jobcode).
 */
public class JobCodeControllerTest extends AbstractSpringMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(JobCodeControllerTest.class);

    private Cookie cookie;
    private int glueJobId;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void prepareData() throws Exception {
        // clean up
        jdbcTemplate.execute("DELETE FROM xxl_job_user WHERE id = 1");
        jdbcTemplate.execute("DELETE FROM xxl_job_group WHERE id = 1");
        jdbcTemplate.execute("DELETE FROM xxl_job_info WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_logglue WHERE id > 0");

        // insert admin user (password = md5("123456"))
        jdbcTemplate.execute("INSERT INTO xxl_job_user(id, username, password, role, permission) "
                + "VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL)");

        // insert a job group (executor) that admin has permission to
        jdbcTemplate.execute("INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time) "
                + "VALUES (1, 'test-executor', 'Test Executor', 0, NULL, NOW())");

        // insert a GLUE(Groovy) type job
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_info(id, job_group, job_desc, add_time, update_time, author, schedule_type, "
                        + "misfire_strategy, glue_type, trigger_status, trigger_last_time, trigger_next_time) "
                        + "VALUES (1, 1, 'GLUE Test Job', NOW(), NOW(), 'tester', 'NONE', 'DO_NOTHING', '"
                        + GlueTypeEnum.GLUE_GROOVY.name()
                        + "', 0, 0, 0)");
        glueJobId = 1;

        // login to get cookie
        MvcResult ret = mockMvc.perform(post("/admin-api/v1/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456"))
                .andReturn();
        cookie = ret.getResponse().getCookie(LoginService.LOGIN_IDENTITY_KEY);
    }

    // ---------------------- index ----------------------

    /**
     * GET /jobcode?jobId={glueJobId} → returns jobcode index view with model attrs
     */
    @Test
    @Disabled
    public void testIndex() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/v1/jobcode")
                        .param("jobId", String.valueOf(glueJobId))
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(view().name("biz/job.code"))
                .andExpect(model().attributeExists("jobInfo", "jobLogGlues", "GlueTypeEnum"))
                .andReturn();

        logger.info(
                "testIndex view: {}",
                result.getModelAndView() != null ? result.getModelAndView().getViewName() : null);
    }

    // ---------------------- save ----------------------

    /**
     * PUT /jobcode/{id} with valid params → code 200
     */
    @Test
    public void testSaveSuccess() throws Exception {
        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobcode/{id}", glueJobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"glueSource\": \"// my groovy source code\", \"glueRemark\": \"initial version remark\"}")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        logger.info("testSaveSuccess: {}", result.getResponse().getContentAsString());
    }

    /**
     * PUT /jobcode/{id} multiple times → glue history is recorded, old ones cleaned
     */
    @Test
    public void testSaveMultipleTimes() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(put("/admin-api/v1/jobcode/{id}", glueJobId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"glueSource\": \"// version " + i + "\", \"glueRemark\": \"remark version " + i
                                    + "\"}")
                            .cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM xxl_job_logglue WHERE job_id = ?", Integer.class, glueJobId);
        logger.info("testSaveMultipleTimes glue log count: {}", count);
        assert count == 5;
    }

    /**
     * PUT /jobcode/{id} → verify glueSource is actually persisted in xxl_job_info
     */
    @Test
    public void testSaveUpdatesGlueSourceInDb() throws Exception {
        String newSource = "// updated groovy source code";

        mockMvc.perform(put("/admin-api/v1/jobcode/{id}", glueJobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"glueSource\": \"" + newSource + "\", \"glueRemark\": \"update remark v1\"}")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String savedSource = jdbcTemplate.queryForObject(
                "SELECT glue_source FROM xxl_job_info WHERE id = ?", String.class, glueJobId);
        assert newSource.equals(savedSource) : "glueSource not persisted, got: " + savedSource;
        logger.info("testSaveUpdatesGlueSourceInDb: glueSource persisted correctly");
    }

    /**
     * PUT /jobcode/{id} with missing glueRemark → code 500
     */
    @Test
    public void testSaveNullGlueRemark() throws Exception {
        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobcode/{id}", glueJobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"glueSource\": \"// source code\"}")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        logger.info("testSaveNullGlueRemark: {}", result.getResponse().getContentAsString());
    }

    /**
     * PUT /jobcode/{id} with remark too short (< 4 chars) → code 500
     */
    @Test
    public void testSaveRemarkTooShort() throws Exception {
        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobcode/{id}", glueJobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"glueSource\": \"// source code\", \"glueRemark\": \"abc\"}")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        logger.info("testSaveRemarkTooShort: {}", result.getResponse().getContentAsString());
    }

    /**
     * PUT /jobcode/{id} with remark too long (> 100 chars) → code 500
     */
    @Test
    public void testSaveRemarkTooLong() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 101; i++) sb.append('a');
        String longRemark = sb.toString();
        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobcode/{id}", glueJobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"glueSource\": \"// source code\", \"glueRemark\": \"" + longRemark + "\"}")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        logger.info("testSaveRemarkTooLong: {}", result.getResponse().getContentAsString());
    }

    /**
     * PUT /jobcode/{id} with non-existent job id → code 500
     */
    @Test
    public void testSaveJobNotFound() throws Exception {
        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobcode/{id}", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"glueSource\": \"// source code\", \"glueRemark\": \"valid remark here\"}")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        logger.info("testSaveJobNotFound: {}", result.getResponse().getContentAsString());
    }
}
