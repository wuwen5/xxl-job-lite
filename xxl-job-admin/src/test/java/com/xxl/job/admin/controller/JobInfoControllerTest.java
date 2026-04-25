package com.xxl.job.admin.controller;

import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.dao.XxlJobGroupDao;
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

import jakarta.servlet.http.Cookie;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class JobInfoControllerTest extends AbstractSpringMvcTest {
    private static Logger logger = LoggerFactory.getLogger(JobInfoControllerTest.class);

    private Cookie cookie;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    XxlJobGroupDao xxlJobGroupDao;

    @BeforeEach
    public void login() throws Exception {
        // 清理并插入测试用户
        jdbcTemplate.execute("delete from xxl_job_user where username='admin'");
        jdbcTemplate.execute("INSERT INTO xxl_job_user(id, username, password, role, permission) VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL)");

        // 确保有测试用的执行器组
        List<XxlJobGroup> existingGroups = xxlJobGroupDao.findAll();
        if (existingGroups == null || existingGroups.isEmpty()) {
            XxlJobGroup jobGroup = new XxlJobGroup();
            jobGroup.setAppname("test-executor");
            jobGroup.setTitle("测试执行器");
            jobGroup.setAddressType(0);
            xxlJobGroupDao.save(jobGroup);
        }

        // 登录获取cookie
        MvcResult ret = mockMvc.perform(
                post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456")
        ).andReturn();
        cookie = ret.getResponse().getCookie(LoginService.LOGIN_IDENTITY_KEY);
        assertNotNull(cookie, "Login should return a cookie");
    }

    @Test
    public void testIndex() throws Exception {
        // 测试首页访问
        MvcResult result = mockMvc.perform(
                get("/jobinfo")
                        .cookie(cookie)
                        .param("jobGroup", "-1")
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull(content);
        logger.info("Index page loaded successfully");
    }

    @Test
    public void testPageList() throws Exception {
        // 测试分页查询
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("start", "0");
        parameters.add("length", "10");
        parameters.add("jobGroup", "-1");
        parameters.add("triggerStatus", "-1");
        parameters.add("jobDesc", "");
        parameters.add("executorHandler", "");
        parameters.add("author", "");

        MvcResult result = mockMvc.perform(
                post("/jobinfo/pageList")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(parameters)
                        .cookie(cookie)
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull(content);
        logger.info("Page list response: {}", content);
        
        // 验证返回的是JSON格式
        assertTrue(content.contains("\"code\"") || content.contains("recordsTotal"));
    }

    @Test
    public void testPageListWithFilters() throws Exception {
        // 测试带过滤条件的分页查询
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("start", "0");
        parameters.add("length", "5");
        parameters.add("jobGroup", "-1");
        parameters.add("triggerStatus", "1");
        parameters.add("jobDesc", "test");
        parameters.add("executorHandler", "");
        parameters.add("author", "admin");

        MvcResult result = mockMvc.perform(
                post("/jobinfo/pageList")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(parameters)
                        .cookie(cookie)
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        logger.info("Filtered page list response: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testAdd() throws Exception {
        // 测试添加任务
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("jobGroup", "1");
        parameters.add("jobDesc", "测试任务");
        parameters.add("author", "admin");
        parameters.add("alarmEmail", "");
        parameters.add("scheduleType", "CRON");
        parameters.add("scheduleConf", "0 0 0 * * ? *");
        parameters.add("misfireStrategy", "DO_NOTHING");
        parameters.add("executorRouteStrategy", "FIRST");
        parameters.add("executorHandler", "demoJobHandler");
        parameters.add("executorParam", "");
        parameters.add("executorBlockStrategy", "SERIAL_EXECUTION");
        parameters.add("executorTimeout", "0");
        parameters.add("executorFailRetryCount", "0");
        parameters.add("glueType", "BEAN");
        parameters.add("glueSource", "");
        parameters.add("glueRemark", "GLUE代码初始化");
        parameters.add("childJobId", "");

        MvcResult result = mockMvc.perform(
                post("/jobinfo/add")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(parameters)
                        .cookie(cookie)
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        logger.info("Add job response: {}", content);
        
        // 验证返回结果
        assertTrue(content.contains("\"code\""));
    }

    @Test
    public void testUpdate() throws Exception {
        // 先添加一个任务用于更新测试
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("测试更新任务");
        jobInfo.setAuthor("admin");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("0 0 0 * * ? *");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        
        // 这里假设服务层已经添加了任务，实际应该先调用add获取ID
        // 为了简化测试，我们直接测试更新接口格式
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("id", "1");
        parameters.add("jobGroup", "1");
        parameters.add("jobDesc", "更新后的任务描述");
        parameters.add("author", "admin");
        parameters.add("scheduleType", "CRON");
        parameters.add("scheduleConf", "0 0 12 * * ? *");
        parameters.add("misfireStrategy", "FIRE_ONCE_NOW");
        parameters.add("executorRouteStrategy", "ROUND");
        parameters.add("executorHandler", "demoJobHandler");
        parameters.add("executorBlockStrategy", "DISCARD_LATER");
        parameters.add("glueType", "BEAN");

        MvcResult result = mockMvc.perform(
                post("/jobinfo/update")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(parameters)
                        .cookie(cookie)
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        logger.info("Update job response: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testRemove() throws Exception {
        // 测试删除任务（使用不存在的ID避免实际删除）
        MvcResult result = mockMvc.perform(
                post("/jobinfo/remove")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "999999")
                        .cookie(cookie)
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        logger.info("Remove job response: {}", content);
        assertTrue(content.contains("\"code\""));
    }

    @Test
    public void testStop() throws Exception {
        // 测试停止任务
        MvcResult result = mockMvc.perform(
                post("/jobinfo/stop")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .cookie(cookie)
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        logger.info("Stop job response: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testStart() throws Exception {
        // 测试启动任务
        MvcResult result = mockMvc.perform(
                post("/jobinfo/start")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .cookie(cookie)
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        logger.info("Start job response: {}", result.getResponse().getContentAsString());
    }

    @Test
    public void testTrigger() throws Exception {
        // 测试手动触发任务
        MvcResult result = mockMvc.perform(
                post("/jobinfo/trigger")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", "1")
                        .param("executorParam", "test param")
                        .param("addressList", "")
                        .cookie(cookie)
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        logger.info("Trigger job response: {}", content);
        assertTrue(content.contains("\"code\""));
    }

    @Test
    public void testNextTriggerTime() throws Exception {
        // 测试计算下次触发时间 - CRON类型
        MvcResult result = mockMvc.perform(
                get("/jobinfo/nextTriggerTime")
                        .param("scheduleType", "CRON")
                        .param("scheduleConf", "0 0 12 * * ? *")
                        .cookie(cookie)
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        logger.info("Next trigger time (CRON) response: {}", content);
        assertTrue(content.contains("\"code\""));
    }

    @Test
    public void testNextTriggerTimeFixedRate() throws Exception {
        // 测试计算下次触发时间 - 固定速率类型
        MvcResult result = mockMvc.perform(
                get("/jobinfo/nextTriggerTime")
                        .param("scheduleType", "FIX_RATE")
                        .param("scheduleConf", "60")
                        .cookie(cookie)
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        logger.info("Next trigger time (FIX_RATE) response: {}", content);
    }

    @Test
    public void testNextTriggerTimeInvalidCron() throws Exception {
        // 测试无效的CRON表达式
        MvcResult result = mockMvc.perform(
                get("/jobinfo/nextTriggerTime")
                        .param("scheduleType", "CRON")
                        .param("scheduleConf", "invalid cron")
                        .cookie(cookie)
        ).andReturn();

        assertEquals(200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        logger.info("Next trigger time (invalid) response: {}", content);
        // 应该返回错误码
        assertTrue(content.contains("\"code\"") && (content.contains("500") || content.contains("fail")));
    }

    @Test
    public void testWithoutAuthentication() throws Exception {
        // 测试未登录访问（应该被拦截）
        MvcResult result = mockMvc.perform(
                post("/jobinfo/pageList")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("start", "0")
                        .param("length", "10")
                        .param("jobGroup", "-1")
                        .param("triggerStatus", "-1")
        ).andReturn();

        // 应该被重定向到登录页或返回错误
        logger.info("Unauthenticated access status: {}", result.getResponse().getStatus());
    }
}
