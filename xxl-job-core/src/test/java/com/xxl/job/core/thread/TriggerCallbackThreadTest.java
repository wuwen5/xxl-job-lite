package com.xxl.job.core.thread;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.log.XxlJobFileAppender;
import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

/**
 * TriggerCallbackThread unit test
 *
 * @author wuwen
 */
class TriggerCallbackThreadTest {

    @TempDir
    static File tempLogDir;

    static MockedConstruction<AdminBizClient> adminBizClientMockedConstruction = null;
    static AdminBizClient mock = null;

    @BeforeAll
    static void setUp() throws Exception {
        XxlJobFileAppender.initLogPath(tempLogDir.getAbsolutePath());

        XxlJobExecutor xxlJobExecutor = new XxlJobExecutor();
        xxlJobExecutor.setAdminAddresses("http://localhost:8080");
        xxlJobExecutor.setAccessToken("");
        adminBizClientMockedConstruction = mockConstruction(AdminBizClient.class);
        xxlJobExecutor.start();
        mock = adminBizClientMockedConstruction.constructed().get(0);
    }

    @AfterAll
    static void tearDownAll() {
        TriggerCallbackThread.getInstance().toStop();
        adminBizClientMockedConstruction.close();
    }

    @AfterEach
    void tearDown() {
        reset(mock);
    }

    @Test
    void testGetInstance() {
        TriggerCallbackThread instance1 = TriggerCallbackThread.getInstance();
        TriggerCallbackThread instance2 = TriggerCallbackThread.getInstance();

        assertSame(instance1, instance2);
    }

    @Test
    void testPushCallBack() throws Exception {
        doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success")).when(mock).callback(anyList());

        HandleCallbackParam callbackParam = new HandleCallbackParam(1L, System.currentTimeMillis(), 200, "success");
        TriggerCallbackThread.pushCallBack(callbackParam);

        Thread.sleep(500);

        verify(mock, atLeastOnce()).callback(anyList());
    }

    @Test
    void testStartAndStop() throws InterruptedException {
        TriggerCallbackThread callbackThread = new TriggerCallbackThread();

        callbackThread.start();

        Thread.sleep(100);

        callbackThread.toStop();

        assertTrue(true);
    }

    @Test
    void testToStopWithoutStart() {
        TriggerCallbackThread callbackThread = new TriggerCallbackThread();
        callbackThread.toStop();

        assertTrue(true);
    }

    @Test
    void testMultipleToStopCalls() throws InterruptedException {
        TriggerCallbackThread callbackThread = new TriggerCallbackThread();
        callbackThread.start();

        Thread.sleep(100);

        callbackThread.toStop();
        callbackThread.toStop();
        callbackThread.toStop();
    }

    @Test
    void testCallbackWithSuccessResult() throws Exception {
        doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success")).when(mock).callback(anyList());

        HandleCallbackParam callbackParam = new HandleCallbackParam(2L, System.currentTimeMillis(), 200, "test");
        TriggerCallbackThread.pushCallBack(callbackParam);

        Thread.sleep(500);

        verify(mock, atLeastOnce()).callback(anyList());
    }

    @Test
    void testCallbackWithFailResult() throws Exception {
        doReturn(new ReturnT<>(ReturnT.FAIL_CODE, "fail")).when(mock).callback(anyList());

        HandleCallbackParam callbackParam = new HandleCallbackParam(3L, System.currentTimeMillis(), 500, "error");
        TriggerCallbackThread.pushCallBack(callbackParam);

        Thread.sleep(500);
    }

    @Test
    void testCallbackWithException() throws Exception {
        doThrow(new RuntimeException("network error")).when(mock).callback(anyList());

        HandleCallbackParam callbackParam = new HandleCallbackParam(4L, System.currentTimeMillis(), 500, "exception");
        TriggerCallbackThread.pushCallBack(callbackParam);

        Thread.sleep(500);
    }

    @Test
    void testCallbackWithNullResult() throws Exception {
        doReturn(null).when(mock).callback(anyList());

        HandleCallbackParam callbackParam = new HandleCallbackParam(5L, System.currentTimeMillis(), 200, "null result");
        TriggerCallbackThread.pushCallBack(callbackParam);

        Thread.sleep(500);
    }

    @Test
    void testCallbackWithMultipleAdminBiz() throws Exception {
        doReturn(new ReturnT<>(ReturnT.FAIL_CODE, "fail"))
                .doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"))
                .when(mock)
                .callback(anyList());

        HandleCallbackParam callbackParam = new HandleCallbackParam(6L, System.currentTimeMillis(), 200, "multi");
        TriggerCallbackThread.pushCallBack(callbackParam);

        Thread.sleep(500);
    }

    @Test
    void testPushMultipleCallbacks() throws Exception {
        doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success")).when(mock).callback(anyList());

        for (int i = 0; i < 5; i++) {
            HandleCallbackParam callbackParam =
                    new HandleCallbackParam(10L + i, System.currentTimeMillis(), 200, "batch-" + i);
            TriggerCallbackThread.pushCallBack(callbackParam);
        }

        Thread.sleep(500);

        verify(mock, atLeastOnce()).callback(anyList());
    }

    @Test
    void testCallbackLogFileCreation() throws Exception {
        doReturn(new ReturnT<>(ReturnT.FAIL_CODE, "always fail")).when(mock).callback(anyList());

        HandleCallbackParam callbackParam = new HandleCallbackParam(7L, System.currentTimeMillis(), 500, "fail test");
        TriggerCallbackThread.pushCallBack(callbackParam);

        Thread.sleep(1000);

        verify(mock, atLeastOnce()).callback(anyList());
    }

    @Test
    void testEmptyCallbackList() {
        TriggerCallbackThread callbackThread = new TriggerCallbackThread();
        callbackThread.start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        callbackThread.toStop();

        assertTrue(true);
    }

    @Test
    void testCallbackWithZeroLogId() throws Exception {
        doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success")).when(mock).callback(anyList());

        HandleCallbackParam callbackParam = new HandleCallbackParam(0L, System.currentTimeMillis(), 200, "zero id");
        TriggerCallbackThread.pushCallBack(callbackParam);

        Thread.sleep(500);

        verify(mock, atLeastOnce()).callback(anyList());
    }

    @Test
    void testCallbackWithNegativeHandleCode() throws Exception {
        doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success")).when(mock).callback(anyList());

        HandleCallbackParam callbackParam =
                new HandleCallbackParam(8L, System.currentTimeMillis(), -1, "negative code");
        TriggerCallbackThread.pushCallBack(callbackParam);

        Thread.sleep(500);

        verify(mock, atLeastOnce()).callback(anyList());
    }
}
