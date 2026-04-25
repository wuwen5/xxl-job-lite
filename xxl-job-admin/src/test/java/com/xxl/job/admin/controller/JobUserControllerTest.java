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
 * Unit tests for JobUserController (/user).
 */
public class JobUserControllerTest extends AbstractSpringMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(JobUserControllerTest.class);

    private Cookie cookie;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void prepareData() throws Exception {
        jdbcTemplate.execute("DELETE FROM xxl_job_user WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_group WHERE id > 0");

        // admin user (md5("123456") = e10adc3949ba59abbe56e057f20f883e)
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_user(id, username, password, role, permission) " +
                "VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL)");

        // a regular user for update/remove tests
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_user(id, username, password, role, permission) " +
                "VALUES (2, 'normaluser', 'e10adc3949ba59abbe56e057f20f883e', 0, NULL)");

        jdbcTemplate.execute(
                "INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time) " +
                "VALUES (1, 'test-executor', 'Test Executor', 0, NULL, NOW())");

        MvcResult ret = mockMvc.perform(
                post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456")
        ).andReturn();
        cookie = ret.getResponse().getCookie(LoginService.LOGIN_IDENTITY_KEY);
    }

    // ---------------------- index ----------------------

    @Test
    public void testIndex() throws Exception {
        mockMvc.perform(get("/user").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(view().name("user/user.index"))
                .andExpect(model().attributeExists("groupList"));
    }

    // ---------------------- pageList ----------------------

    @Test
    public void testPageList() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/pageList")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("start", "0")
                                .param("length", "10")
                                .param("username", "")
                                .param("role", "-1")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsTotal").isNumber())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();
        logger.info("testPageList: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testPageListPasswordIsHidden() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/pageList")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("start", "0")
                                .param("length", "10")
                                .param("username", "admin")
                                .param("role", "-1")
                                .cookie(cookie))
                .andExpect(status().isOk())
                // password must be null (hidden)
                .andExpect(jsonPath("$.data[0].password").doesNotExist())
                .andReturn();
        logger.info("testPageListPasswordIsHidden: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- add ----------------------

    @Test
    public void testAddSuccess() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/add")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("username", "newuser")
                                .param("password", "password123")
                                .param("role", "0")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testAddSuccess: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testAddUsernameTooShort() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/add")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("username", "ab")   // < 4 chars
                                .param("password", "password123")
                                .param("role", "0")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testAddUsernameTooShort: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testAddPasswordTooShort() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/add")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("username", "validuser")
                                .param("password", "ab")  // < 4 chars
                                .param("role", "0")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testAddPasswordTooShort: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testAddDuplicateUsername() throws Exception {
        // 'admin' already exists
        MvcResult result = mockMvc.perform(
                        post("/user/add")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("username", "admin")
                                .param("password", "password123")
                                .param("role", "0")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testAddDuplicateUsername: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testAddUsernameEmpty() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/add")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("username", "")
                                .param("password", "password123")
                                .param("role", "0")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testAddUsernameEmpty: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- update ----------------------

    @Test
    public void testUpdateSuccess() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/update")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("id", "2")
                                .param("username", "normaluser")
                                .param("password", "newpassword")
                                .param("role", "0")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testUpdateSuccess: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testUpdatePasswordTooShort() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/update")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("id", "2")
                                .param("username", "normaluser")
                                .param("password", "ab")  // < 4 chars
                                .param("role", "0")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testUpdatePasswordTooShort: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testUpdateSelfNotAllowed() throws Exception {
        // admin tries to update themselves → should fail
        MvcResult result = mockMvc.perform(
                        post("/user/update")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("id", "1")
                                .param("username", "admin")
                                .param("password", "newpassword")
                                .param("role", "1")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testUpdateSelfNotAllowed: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testUpdateWithoutPassword() throws Exception {
        // empty password → keep existing password (password=null in update)
        MvcResult result = mockMvc.perform(
                        post("/user/update")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("id", "2")
                                .param("username", "normaluser")
                                .param("password", "")
                                .param("role", "0")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testUpdateWithoutPassword: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- remove ----------------------

    @Test
    public void testRemoveSuccess() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/remove")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("id", "2")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testRemoveSuccess: {}", result.getResponse().getContentAsString());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM xxl_job_user WHERE id = 2", Integer.class);
        assert count == 0 : "User id=2 should have been deleted";
    }

    @Test
    public void testRemoveSelfNotAllowed() throws Exception {
        // admin tries to delete themselves → should fail
        MvcResult result = mockMvc.perform(
                        post("/user/remove")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("id", "1")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testRemoveSelfNotAllowed: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- updatePwd ----------------------

    @Test
    public void testUpdatePwdSuccess() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/updatePwd")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("oldPassword", "123456")
                                .param("password", "newpass1")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        logger.info("testUpdatePwdSuccess: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testUpdatePwdWrongOldPassword() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/updatePwd")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("oldPassword", "wrongpassword")
                                .param("password", "newpass1")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testUpdatePwdWrongOldPassword: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testUpdatePwdNewPasswordTooShort() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/updatePwd")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("oldPassword", "123456")
                                .param("password", "ab")  // < 4 chars
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testUpdatePwdNewPasswordTooShort: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testUpdatePwdOldPasswordEmpty() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/user/updatePwd")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("oldPassword", "")
                                .param("password", "newpass1")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();
        logger.info("testUpdatePwdOldPasswordEmpty: {}", result.getResponse().getContentAsString());
    }
}

