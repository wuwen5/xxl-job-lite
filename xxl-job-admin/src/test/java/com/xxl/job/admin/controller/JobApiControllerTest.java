package com.xxl.job.admin.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.JobExecutorInitParam;
import com.xxl.job.core.biz.model.JobExecutorParam;
import com.xxl.job.core.biz.model.JobInfoParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.util.GsonTool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Unit tests for {@link JobApiController}.
 */
public class JobApiControllerTest extends AbstractSpringMvcTest {

    private static final Logger logger = LoggerFactory.getLogger(JobApiControllerTest.class);

    // ---------------------- validation tests ----------------------

    /**
     * Non-POST request → FAIL_CODE
     */
    @Test
    public void testGetMethodNotAllowed() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/callback").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        logger.info("testGetMethodNotAllowed: {}", result.getResponse().getContentAsString());
    }

    /**
     * Unknown URI → FAIL_CODE
     */
    @Test
    public void testUnknownUri() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/unknownUri")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        logger.info("testUnknownUri: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- callback ----------------------

    /**
     * POST /api/callback with valid params → SUCCESS_CODE
     */
    @Test
    public void testCallback() throws Exception {
        HandleCallbackParam param = new HandleCallbackParam();
        param.setLogId(1);
        param.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);

        List<HandleCallbackParam> paramList = Arrays.asList(param);
        String body = GsonTool.toJson(paramList);

        MvcResult result = mockMvc.perform(post("/api/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        logger.info("testCallback: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /api/callback with empty list → still returns a response (not 5xx HTTP)
     */
    @Test
    public void testCallbackEmpty() throws Exception {
        String body = GsonTool.toJson(new ArrayList<>());

        MvcResult result = mockMvc.perform(post("/api/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        logger.info("testCallbackEmpty: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- registry ----------------------

    /**
     * POST /api/registry with valid params → SUCCESS_CODE
     */
    @Test
    public void testRegistry() throws Exception {
        RegistryParam param = new RegistryParam(
                RegistryConfig.RegistType.EXECUTOR.name(), "xxl-job-executor-example", "127.0.0.1:9999");
        String body = GsonTool.toJson(param);

        MvcResult result = mockMvc.perform(post("/api/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        logger.info("testRegistry: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /api/registry with missing fields → FAIL_CODE (validation)
     */
    @Test
    public void testRegistryInvalidParam() throws Exception {
        RegistryParam param = new RegistryParam("", "", "");
        String body = GsonTool.toJson(param);

        MvcResult result = mockMvc.perform(post("/api/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        logger.info("testRegistryInvalidParam: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- registryRemove ----------------------

    /**
     * POST /api/registryRemove with valid params → SUCCESS_CODE
     */
    @Test
    public void testRegistryRemove() throws Exception {
        RegistryParam param = new RegistryParam(
                RegistryConfig.RegistType.EXECUTOR.name(), "xxl-job-executor-example", "127.0.0.1:9999");
        String body = GsonTool.toJson(param);

        MvcResult result = mockMvc.perform(post("/api/registryRemove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        logger.info("testRegistryRemove: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /api/registryRemove with missing fields → FAIL_CODE (validation)
     */
    @Test
    public void testRegistryRemoveInvalidParam() throws Exception {
        RegistryParam param = new RegistryParam("", "", "");
        String body = GsonTool.toJson(param);

        MvcResult result = mockMvc.perform(post("/api/registryRemove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andReturn();

        logger.info("testRegistryRemoveInvalidParam: {}", result.getResponse().getContentAsString());
    }

    // ---------------------- initJobInfo ----------------------

    /**
     * POST /api/initJobInfo with valid params → SUCCESS_CODE
     */
    @Test
    public void testInitJobInfo() throws Exception {
        List<JobInfoParam> jobInfoParamList = new ArrayList<>();
        jobInfoParamList.add(
                new JobInfoParam("xxl-job-executor-example", "测试任务", "demoJobHandler", "", "0/5 * * * * ?", -1));

        JobExecutorInitParam initParam = new JobExecutorInitParam();
        initParam.setJobExecutorParam(new JobExecutorParam("xxl-job-executor-example", "测试执行器"));
        initParam.setJobInfoParamList(jobInfoParamList);

        String body = GsonTool.toJson(initParam);

        MvcResult result = mockMvc.perform(post("/api/initJobInfo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        logger.info("testInitJobInfo: {}", result.getResponse().getContentAsString());
    }

    /**
     * POST /api/initJobInfo with null body → handled gracefully
     */
    @Test
    public void testInitJobInfoNullBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/initJobInfo").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        logger.info("testInitJobInfoNullBody: {}", result.getResponse().getContentAsString());
    }
}
