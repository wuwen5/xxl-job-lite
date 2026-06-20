package com.xxl.job.core.thread;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.core.biz.model.JobInfoParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.XxlJobExecutor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * ExecutorRegistryThread unit test
 *
 * @author wuwen
 */
class ExecutorRegistryThreadTest {

    static MockedConstruction<AdminBizClient> adminBizClientMockedConstruction = null;
    static AdminBizClient mock = null;

    @BeforeAll
    static void setUp() throws Exception {
        XxlJobExecutor xxlJobExecutor = new XxlJobExecutor();
        xxlJobExecutor.setAdminAddresses("http://localhost:8080");
        xxlJobExecutor.setAccessToken("");
        adminBizClientMockedConstruction = mockConstruction(AdminBizClient.class);
        xxlJobExecutor.start();
        mock = adminBizClientMockedConstruction.constructed().get(0);
    }

    @AfterAll
    static void tearDownAll() {
        adminBizClientMockedConstruction.close();
    }

    @AfterEach
    void tearDown() {
        // Stop the thread if it's running
        ExecutorRegistryThread.getInstance().toStop();
    }

    @Test
    void testGetInstance() {
        ExecutorRegistryThread instance1 = ExecutorRegistryThread.getInstance();
        ExecutorRegistryThread instance2 = ExecutorRegistryThread.getInstance();

        // Should return same singleton instance
        assertSame(instance1, instance2);
    }

    @Test
    void testStartAndStop() throws Exception {

        Mockito.when(mock.registry(argThat(a -> a != null && a.getRegistryKey().equals("test-app"))))
                .thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));
        clearInvocations(mock);

        ExecutorRegistryThread registryThread = ExecutorRegistryThread.getInstance();

        // Start the thread
        registryThread.start("test-app", "http://localhost:8080");

        // Give thread time to start and register
        Thread.sleep(1500);

        // Stop the thread
        registryThread.toStop();

        verify(mock, atLeastOnce()).registry(any(RegistryParam.class));
        verify(mock, atLeastOnce()).registryRemove(any(RegistryParam.class));
    }

    @Test
    void testStartWithRegistryException() throws Exception {

        when(mock.registry(argThat(a -> a != null && a.getRegistryKey().equals("test-app-error"))))
                .thenThrow(new RuntimeException("network error"));

        ExecutorRegistryThread registryThread = ExecutorRegistryThread.getInstance();

        // Start the thread - should handle exception gracefully
        registryThread.start("test-app-error", "http://localhost:8082");

        // Give thread time to attempt registration
        Thread.sleep(1500);

        // Stop the thread
        registryThread.toStop();
    }

    @Test
    void testToStopWithoutStart() {
        ExecutorRegistryThread registryThread = ExecutorRegistryThread.getInstance();

        // Calling toStop without start should not throw exception
        registryThread.toStop();
    }

    @Test
    void testMultipleToStopCalls() throws Exception {

        ExecutorRegistryThread registryThread = ExecutorRegistryThread.getInstance();

        Mockito.when(mock.registry(any())).thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));

        // Start the thread
        registryThread.start("test-app-stop", "http://localhost:8083");

        Thread.sleep(500);

        // Multiple stop calls should not cause issues
        registryThread.toStop();
        registryThread.toStop();
        registryThread.toStop();
    }

    @Test
    void testInitJobInfoInitParams() throws Exception {
        Mockito.when(mock.initJobInfo(argThat(
                        a -> a != null && a.getJobExecutorParam().getAppName().equals("test-app-init"))))
                .thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));

        ExecutorRegistryThread registryThread = ExecutorRegistryThread.getInstance();

        // Initialize job info params
        List<JobInfoParam> jobInfoParams = new ArrayList<>();
        JobInfoParam jobInfoParam = new JobInfoParam();
        jobInfoParam.setAppName("test-app");
        jobInfoParam.setJobDesc("Test Job");
        jobInfoParam.setExecutorHandler("testHandler");
        jobInfoParams.add(jobInfoParam);

        registryThread.initJobInfoInitParams(jobInfoParams);

        // Start the thread
        registryThread.start("test-app-init", "http://localhost:8084");

        // Give thread time to initialize job info
        Thread.sleep(1000);

        // Stop the thread
        registryThread.toStop();

        // Verify initJobInfo was called
        verify(mock, atLeastOnce()).initJobInfo(any());
    }

    @Test
    void testInitJobInfoWithEmptyParams() throws Exception {

        clearInvocations(mock);

        ExecutorRegistryThread registryThread = ExecutorRegistryThread.getInstance();

        // Initialize with empty params
        registryThread.initJobInfoInitParams(new ArrayList<>());

        // Start the thread
        registryThread.start("test-app-empty", "http://localhost:8085");

        // Give thread time to start
        Thread.sleep(1000);

        // Stop the thread
        registryThread.toStop();

        // initJobInfo should not be called with empty params
        verify(mock, never()).initJobInfo(any());
    }

    @Test
    void testInitJobInfoWithFailure() throws Exception {

        when(mock.initJobInfo(argThat(
                        a -> a != null && a.getJobExecutorParam().getAppName().equals("test-app-fail"))))
                .thenReturn(new ReturnT<>(ReturnT.FAIL_CODE, "init failed"));

        ExecutorRegistryThread registryThread = ExecutorRegistryThread.getInstance();

        // Initialize job info params
        List<JobInfoParam> jobInfoParams = new ArrayList<>();
        JobInfoParam jobInfoParam = new JobInfoParam();
        jobInfoParam.setAppName("test-app");
        jobInfoParams.add(jobInfoParam);

        registryThread.initJobInfoInitParams(jobInfoParams);

        // Start the thread - should handle failure gracefully
        registryThread.start("test-app-fail", "http://localhost:8086");

        // Give thread time to attempt initialization
        Thread.sleep(1000);

        // Stop the thread
        registryThread.toStop();

        // Verify initJobInfo was called but failed
        verify(mock, timeout(2000)).initJobInfo(any());
    }

    @Test
    void testInitJobInfoWithException() throws Exception {

        when(mock.initJobInfo(argThat(
                        a -> a != null && a.getJobExecutorParam().getAppName().equals("test-app-exception"))))
                .thenThrow(new RuntimeException("init error"));

        clearInvocations(mock);

        ExecutorRegistryThread registryThread = ExecutorRegistryThread.getInstance();

        // Initialize job info params
        List<JobInfoParam> jobInfoParams = new ArrayList<>();
        JobInfoParam jobInfoParam = new JobInfoParam();
        jobInfoParam.setAppName("test-app");
        jobInfoParams.add(jobInfoParam);

        registryThread.initJobInfoInitParams(jobInfoParams);

        // Start the thread - should handle exception gracefully
        registryThread.start("test-app-exception", "http://localhost:8087");

        // Give thread time to attempt initialization
        Thread.sleep(1000);

        // Stop the thread
        registryThread.toStop();

        // Verify initJobInfo was attempted
        verify(mock, timeout(2000)).initJobInfo(any());
    }
}
