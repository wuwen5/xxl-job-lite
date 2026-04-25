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
import static org.hamcrest.Matchers.*;

public class IndexControllerTest extends AbstractSpringMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(IndexControllerTest.class);

    private Cookie cookie;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void login() throws Exception {
        jdbcTemplate.execute("DELETE FROM xxl_job_user WHERE id = 1");
        jdbcTemplate.execute("INSERT INTO xxl_job_user(id, username, password, role, permission) VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL)");

        MvcResult ret = mockMvc.perform(
                post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456")
        ).andReturn();
        cookie = ret.getResponse().getCookie(LoginService.LOGIN_IDENTITY_KEY);
    }

    /**
     * GET / with valid login cookie → returns index view with dashboard data
     */
    @Test
    public void testIndex() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("jobInfoCount", "jobLogCount", "jobLogSuccessCount", "executorCount"))
                .andReturn();

        logger.info("testIndex model: {}", result.getModelAndView() != null ? result.getModelAndView().getModel() : null);
    }

    /**
     * GET /chartInfo with date range → returns JSON with chart data
     */
    @Test
    public void testChartInfo() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/chartInfo")
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

    /**
     * GET /toLogin without cookie → returns login view
     */
    @Test
    public void testToLoginNotLoggedIn() throws Exception {
        mockMvc.perform(get("/toLogin"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    /**
     * GET /toLogin with valid cookie → redirects to /
     */
    @Test
    public void testToLoginAlreadyLoggedIn() throws Exception {
        mockMvc.perform(get("/toLogin").cookie(cookie))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * POST /login with correct credentials → code 200 and cookie set
     */
    @Test
    public void testLoginDoSuccess() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("userName", "admin")
                                .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        logger.info("testLoginDoSuccess response: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /login with wrong password → code != 200
     */
    @Test
    public void testLoginDoFail() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("userName", "admin")
                                .param("password", "wrongpassword"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(not(200)))
                .andReturn();

        logger.info("testLoginDoFail response: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /logout with valid cookie → code 200
     */
    @Test
    public void testLogout() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/logout")
                                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        logger.info("testLogout response: {}", result.getResponse().getContentAsString());
    }

    /**
     * GET /help with valid cookie → returns help view
     */
    @Test
    public void testHelp() throws Exception {
        mockMvc.perform(get("/help").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(view().name("help"));
    }
}

