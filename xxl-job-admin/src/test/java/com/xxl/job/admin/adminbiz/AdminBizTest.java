package com.xxl.job.admin.adminbiz;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xxl.job.admin.AbstractTest;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.JobExecutorInitParam;
import com.xxl.job.core.biz.model.JobExecutorParam;
import com.xxl.job.core.biz.model.JobInfoParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.enums.RegistryConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * admin api test
 *
 * @author xuxueli 2017-07-28 22:14:52
 */
public class AdminBizTest extends AbstractTest {

    // admin-client
    private static String addressUrl = "http://127.0.0.1:%d/xxl-job-admin";
    private static String accessToken = null;
    private static int timeoutSecond = 3;

    @Autowired
    private Environment environment;

    private int port() {
        return this.environment.getProperty("local.server.port", Integer.class, -1);
    }

    @Test
    public void callback() {
        AdminBiz adminBiz = new AdminBizClient(String.format(addressUrl, port()), accessToken, timeoutSecond);

        HandleCallbackParam param = new HandleCallbackParam();
        param.setLogId(1);
        param.setHandleCode(XxlJobContext.HANDLE_CODE_SUCCESS);

        List<HandleCallbackParam> callbackParamList = Arrays.asList(param);

        ReturnT<String> returnT = adminBiz.callback(callbackParamList);

        assertEquals(ReturnT.SUCCESS_CODE, returnT.getCode());
    }

    /**
     * registry executor
     *
     */
    @Test
    public void registry() {
        AdminBiz adminBiz = new AdminBizClient(String.format(addressUrl, port()), accessToken, timeoutSecond);

        RegistryParam registryParam = new RegistryParam(
                RegistryConfig.RegistType.EXECUTOR.name(), "xxl-job-executor-example", "127.0.0.1:9999");
        ReturnT<String> returnT = adminBiz.registry(registryParam);

        assertEquals(ReturnT.SUCCESS_CODE, returnT.getCode());
    }

    /**
     * registry executor remove
     *
     */
    @Test
    public void registryRemove() {
        AdminBiz adminBiz = new AdminBizClient(String.format(addressUrl, port()), accessToken, timeoutSecond);

        RegistryParam registryParam = new RegistryParam(
                RegistryConfig.RegistType.EXECUTOR.name(), "xxl-job-executor-example", "127.0.0.1:9999");
        ReturnT<String> returnT = adminBiz.registryRemove(registryParam);

        assertEquals(ReturnT.SUCCESS_CODE, returnT.getCode());
    }

    @Test
    public void initJobInfo() {
        AdminBiz adminBiz = new AdminBizClient(String.format(addressUrl, port()), accessToken, timeoutSecond);

        List<JobInfoParam> jobInfoParamList = new ArrayList<>();
        jobInfoParamList.add(
                new JobInfoParam("xxl-job-executor-example", "测试", "demoJobHandler", "", "0/5 * * * * ?", -1));
        JobExecutorInitParam initParam = new JobExecutorInitParam();
        initParam.setJobExecutorParam(new JobExecutorParam("xxl-job-executor-example", "测试执行器"));
        initParam.setJobInfoParamList(jobInfoParamList);
        ReturnT<String> returnT = adminBiz.initJobInfo(initParam);

        assertEquals(ReturnT.SUCCESS_CODE, returnT.getCode());
    }
}
