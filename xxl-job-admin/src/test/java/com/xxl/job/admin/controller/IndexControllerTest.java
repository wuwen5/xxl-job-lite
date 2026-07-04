package com.xxl.job.admin.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

public class IndexControllerTest extends AbstractSpringMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(IndexControllerTest.class);

    private Cookie cookie;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void login() throws Exception {
        jdbcTemplate.execute("DELETE FROM xxl_job_user WHERE id = 1");
        jdbcTemplate.execute(
                "INSERT INTO xxl_job_user(id, username, password, role, permission) VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL)");

        MvcResult ret = mockMvc.perform(post("/admin-api/v1/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456"))
                .andReturn();
        cookie = ret.getResponse().getCookie(LoginService.LOGIN_IDENTITY_KEY);
    }

    @Test
    public void testUserInfo() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/v1/userinfo").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.content.id").value(1))
                .andExpect(jsonPath("$.content.username").value("admin"))
                .andExpect(jsonPath("$.content.role").value(1))
                .andExpect(jsonPath("$.content.permission").value(""))
                .andExpect(jsonPath("$.content.password").doesNotExist())
                .andReturn();

        logger.info("testUserInfo response: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testDashboard() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/v1/dashboard").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        logger.info("testDashboard response: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testChartInfo() throws Exception {
        MvcResult result = mockMvc.perform(get("/admin-api/v1/chart")
                        .cookie(cookie)
                        .param("startDate", "2026-04-12 00:00:00")
                        .param("endDate", "2026-04-19 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.content.triggerDayList").isArray())
                .andExpect(jsonPath("$.content.triggerDayCountRunningList").isArray())
                .andExpect(jsonPath("$.content.triggerDayCountSucList").isArray())
                .andExpect(jsonPath("$.content.triggerDayCountFailList").isArray())
                .andReturn();

        logger.info("testChartInfo response: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testLoginDoSuccess() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        logger.info("testLoginDoSuccess response: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testLoginDoFail() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "wrongpassword"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(not(200)))
                .andReturn();

        logger.info("testLoginDoFail response: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testLogout() throws Exception {
        MvcResult result = mockMvc.perform(post("/admin-api/v1/logout").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        logger.info("testLogout response: {}", result.getResponse().getContentAsString());
    }
}
