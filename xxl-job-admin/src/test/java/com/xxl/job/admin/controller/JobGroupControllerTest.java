package com.xxl.job.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.xxl.job.admin.service.impl.LoginService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Unit tests for JobGroupController (/jobgroup).
 */
public class JobGroupControllerTest extends AbstractSpringMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(JobGroupControllerTest.class);

    private Cookie cookie;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void prepareData() throws Exception {
        jdbcTemplate.execute("DELETE FROM xxl_job_user WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_info WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_group WHERE id > 0");

        // admin user (password = md5("123456"))
        jdbcTemplate.execute("INSERT INTO xxl_job_user(id, username, password, role, permission) "
                + "VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL)");

        // existing group used as data baseline
        jdbcTemplate.execute("INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time) "
                + "VALUES (1, 'existing-executor', 'Existing Executor', 0, NULL, NOW())");

        MvcResult ret = mockMvc.perform(post("/admin-api/v1/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456"))
                .andReturn();
        cookie = ret.getResponse().getCookie(LoginService.LOGIN_IDENTITY_KEY);
    }

    // ---------------------- pageList ----------------------

    @Test
    public void testPageList() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup/pageList")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("start", "0")
                        .param("length", "10")
                        .param("appname", "")
                        .param("title", "")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsTotal").isNumber())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();
        logger.info("testPageList: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testPageListWithFilter() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup/pageList")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("start", "0")
                        .param("length", "10")
                        .param("appname", "existing-executor")
                        .param("title", "")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsTotal").value(1))
                .andReturn();
        logger.info("testPageListWithFilter: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- save ----------------------

    @Test
    public void testSaveSuccess() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appname", "new-executor")
                        .param("title", "New Executor")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testSaveSuccess: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testSaveAppnameTooShort() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appname", "ab") // < 4 chars
                        .param("title", "Some Title")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testSaveAppnameTooShort: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testSaveAppnameEmpty() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appname", "")
                        .param("title", "Some Title")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testSaveAppnameEmpty: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testSaveTitleEmpty() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appname", "valid-name")
                        .param("title", "")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testSaveTitleEmpty: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testSaveManualAddressTypeWithAddresses() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appname", "manual-executor")
                        .param("title", "Manual Executor")
                        .param("addressType", "1")
                        .param("addressList", "127.0.0.1:9999,127.0.0.1:9998")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info(
                "testSaveManualAddressTypeWithAddresses: {}",
                result.getResponse().getContentAsString());
    }

    @Test
    public void testSaveManualAddressTypeEmptyAddressList() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appname", "manual-exec2")
                        .param("title", "Manual2")
                        .param("addressType", "1")
                        .param("addressList", "") // missing
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info(
                "testSaveManualAddressTypeEmptyAddressList: {}",
                result.getResponse().getContentAsString());
    }

    /**
     * POST /jobgroup/save → AppName contains invalid characters (< or >) → code 500
     */
    @Test
    public void testSaveAppnameWithInvalidChars() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appname", "invalid<name")
                        .param("title", "Some Title")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testSaveAppnameWithInvalidChars: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /jobgroup/save → Title contains invalid characters → code 500
     */
    @Test
    public void testSaveTitleWithInvalidChars() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appname", "valid-name")
                        .param("title", "Invalid>Title")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testSaveTitleWithInvalidChars: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /jobgroup/save → AppName too long (>64 chars) → code 500
     */
    @Test
    public void testSaveAppnameTooLong() throws Exception {
        String longAppname = "a".repeat(65);
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appname", longAppname)
                        .param("title", "Some Title")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testSaveAppnameTooLong: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /jobgroup/save → Manual address type with empty item in address list → code 500
     */
    @Test
    public void testSaveManualAddressWithEmptyItem() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appname", "manual-exec3")
                        .param("title", "Manual3")
                        .param("addressType", "1")
                        .param("addressList", "127.0.0.1:9999,,127.0.0.1:9998") // empty item
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info(
                "testSaveManualAddressWithEmptyItem: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /jobgroup/save → Manual address type with invalid chars in address list → code 500
     */
    @Test
    public void testSaveManualAddressWithInvalidChars() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/jobgroup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("appname", "manual-exec4")
                        .param("title", "Manual4")
                        .param("addressType", "1")
                        .param("addressList", "127.0.0.1:9999,<invalid>:9998")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info(
                "testSaveManualAddressWithInvalidChars: {}",
                result.getResponse().getContentAsString());
    }

    // ---------------------- update ----------------------

    @Test
    public void testUpdateSuccess() throws Exception {
        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobgroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .param("appname", "existing-executor")
                        .param("title", "Updated Executor Title")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testUpdateSuccess: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testUpdateAppnameEmpty() throws Exception {
        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobgroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .param("appname", "")
                        .param("title", "Title")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testUpdateAppnameEmpty: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /jobgroup/update → AppName too short → code 500
     */
    @Test
    public void testUpdateAppnameTooShort() throws Exception {
        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobgroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .param("appname", "ab")
                        .param("title", "Title")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testUpdateAppnameTooShort: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /jobgroup/update → Title empty → code 500
     */
    @Test
    public void testUpdateTitleEmpty() throws Exception {
        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobgroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .param("appname", "existing-executor")
                        .param("title", "")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testUpdateTitleEmpty: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /jobgroup/update → Manual address type with empty address list → code 500
     */
    @Test
    public void testUpdateManualAddressEmpty() throws Exception {
        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobgroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .param("appname", "existing-executor")
                        .param("title", "Updated Title")
                        .param("addressType", "1")
                        .param("addressList", "")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testUpdateManualAddressEmpty: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /jobgroup/update → Manual address type with empty item → code 500
     */
    @Test
    public void testUpdateManualAddressWithEmptyItem() throws Exception {
        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobgroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .param("appname", "existing-executor")
                        .param("title", "Updated Title")
                        .param("addressType", "1")
                        .param("addressList", "127.0.0.1:9999,,127.0.0.1:9998")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info(
                "testUpdateManualAddressWithEmptyItem: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /jobgroup/update → Auto register type (addressType=0) with registry data
     */
    @Test
    public void testUpdateAutoRegisterWithRegistry() throws Exception {
        // Insert registry data for the appname
        jdbcTemplate.execute("INSERT INTO xxl_job_registry(registry_group, registry_key, registry_value, update_time) "
                + "VALUES ('EXECUTOR', 'existing-executor', '127.0.0.1:9999', NOW())");

        MvcResult result = mockMvc.perform(put("/admin-api/v1/jobgroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .param("appname", "existing-executor")
                        .param("title", "Updated With Registry")
                        .param("addressType", "0")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info(
                "testUpdateAutoRegisterWithRegistry: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- remove ----------------------

    @Test
    public void testRemoveFailWhenJobsExist() throws Exception {
        // insert a job under this group
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_info(id, job_group, job_desc, add_time, update_time, author, schedule_type, "
                        + "misfire_strategy, glue_type, trigger_status, trigger_last_time, trigger_next_time) "
                        + "VALUES (1, 1, 'Test Job', NOW(), NOW(), 'tester', 'NONE', 'DO_NOTHING', 'BEAN', 0, 0, 0)");

        MvcResult result = mockMvc.perform(delete("/admin-api/v1/jobgroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testRemoveFailWhenJobsExist: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testRemoveFailWhenOnlyOneGroup() throws Exception {
        // group id=1 exists, no jobs → but it's the only group → should fail
        MvcResult result = mockMvc.perform(delete("/admin-api/v1/jobgroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testRemoveFailWhenOnlyOneGroup: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testRemoveSuccess() throws Exception {
        // insert a second group so removal of id=1 is allowed
        jdbcTemplate.execute("INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time) "
                + "VALUES (2, 'second-executor', 'Second Executor', 0, NULL, NOW())");

        MvcResult result = mockMvc.perform(delete("/admin-api/v1/jobgroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testRemoveSuccess: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /jobgroup/remove → Remove non-existent group → code 500 (ret <= 0)
     */
    @Test
    public void testRemoveNonExistentGroup() throws Exception {
        // Insert a second group first to avoid "only one group" check
        jdbcTemplate.execute("INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time) "
                + "VALUES (2, 'second-executor', 'Second Executor', 0, NULL, NOW())");

        MvcResult result = mockMvc.perform(delete("/admin-api/v1/jobgroup/9999")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "9999") // non-existent
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testRemoveNonExistentGroup: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- loadById ----------------------

    @Test
    public void testLoadByIdFound() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/v1/jobgroup/1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.content.appname").value("existing-executor"))
                .andReturn();
        logger.info("testLoadByIdFound: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testLoadByIdNotFound() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/v1/jobgroup/9999")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "9999")
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testLoadByIdNotFound: {}", result.getResponse().getContentAsString());
    }
}
