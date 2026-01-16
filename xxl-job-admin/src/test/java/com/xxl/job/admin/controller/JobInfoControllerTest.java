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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class JobInfoControllerTest extends AbstractSpringMvcTest {
    private static Logger logger = LoggerFactory.getLogger(JobInfoControllerTest.class);

    private Cookie cookie;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void login() throws Exception {

        jdbcTemplate.execute("INSERT INTO xxl_job_user(id, username, password, role, permission) VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL)");

        MvcResult ret = mockMvc.perform(
                post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456")
        ).andReturn();
        cookie = ret.getResponse().getCookie(LoginService.LOGIN_IDENTITY_KEY);
    }

    @Test
    public void testAdd() throws Exception {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("jobGroup", "1");
        parameters.add("triggerStatus", "-1");

        MvcResult ret = mockMvc.perform(
                post("/jobinfo/pageList")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        //.content(paramsJson)
                        .params(parameters)
                        .cookie(cookie)
        ).andReturn();

        logger.info(ret.getResponse().getContentAsString());
    }

}
