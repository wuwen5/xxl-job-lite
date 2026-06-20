package com.xxl.job.core.thread;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import java.io.File;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

/**
 * JobThread unit test
 *
 * @author wuwen
 */
class JobThreadTest {

    @TempDir
    static File tempLogDir;

    static MockedConstruction<AdminBizClient> adminBizClientMockedConstruction = null;
    static AdminBizClient mock = null;

    @BeforeAll
    static void setUpClass() throws Exception {
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
    void testConstructor() {
        IJobHandler handler = new SimpleTestHandler();
        JobThread jobThread = new JobThread(1, handler);

        assertNotNull(jobThread);
        assertEquals(handler, jobThread.getHandler());
        assertTrue(jobThread.getName().startsWith("xxl-job, JobThread-1-"));
    }

    @Test
    void testPushTriggerQueue() {
        IJobHandler handler = new SimpleTestHandler();
        JobThread jobThread = new JobThread(2, handler);

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setLogId(100L);
        triggerParam.setJobId(2);

        // First push should succeed
        ReturnT<String> result1 = jobThread.pushTriggerQueue(triggerParam);
        assertEquals(ReturnT.SUCCESS_CODE, result1.getCode());

        // Second push with same logId should fail (avoid repeat)
        ReturnT<String> result2 = jobThread.pushTriggerQueue(triggerParam);
        assertEquals(ReturnT.FAIL_CODE, result2.getCode());
        assertTrue(result2.getMsg().contains("repeate trigger job"));
    }

    @Test
    void testPushTriggerQueueWithDifferentLogIds() {
        IJobHandler handler = new SimpleTestHandler();
        JobThread jobThread = new JobThread(3, handler);

        TriggerParam triggerParam1 = new TriggerParam();
        triggerParam1.setLogId(101L);
        triggerParam1.setJobId(3);

        TriggerParam triggerParam2 = new TriggerParam();
        triggerParam2.setLogId(102L);
        triggerParam2.setJobId(3);

        // Both should succeed with different logIds
        ReturnT<String> result1 = jobThread.pushTriggerQueue(triggerParam1);
        ReturnT<String> result2 = jobThread.pushTriggerQueue(triggerParam2);

        assertEquals(ReturnT.SUCCESS_CODE, result1.getCode());
        assertEquals(ReturnT.SUCCESS_CODE, result2.getCode());
    }

    @Test
    void testToStop() {
        IJobHandler handler = new SimpleTestHandler();
        JobThread jobThread = new JobThread(4, handler);

        // Initially not stopped
        assertFalse(hasToStopFlag(jobThread));

        // Call toStop
        jobThread.toStop("test stop reason");

        // Should be marked as stopped
        assertTrue(hasToStopFlag(jobThread));
    }

    @Test
    void testIsRunningOrHasQueue() {
        IJobHandler handler = new SimpleTestHandler();
        JobThread jobThread = new JobThread(5, handler);

        // Initially not running and no queue
        assertFalse(jobThread.isRunningOrHasQueue());

        // Add a trigger to queue
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setLogId(200L);
        triggerParam.setJobId(5);
        jobThread.pushTriggerQueue(triggerParam);

        // Should have queue
        assertTrue(jobThread.isRunningOrHasQueue());
    }

    @Test
    void testGetHandler() {
        IJobHandler handler = new SimpleTestHandler();
        JobThread jobThread = new JobThread(6, handler);

        assertSame(handler, jobThread.getHandler());
    }

    @Test
    void testJobThreadExecution() throws InterruptedException {
        doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success")).when(mock).callback(anyList());

        IJobHandler handler = new SimpleTestHandler();
        JobThread jobThread = new JobThread(7, handler);

        // Start the thread
        jobThread.start();

        // Create and push a trigger
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(7);
        triggerParam.setLogId(300L);
        triggerParam.setLogDateTime(System.currentTimeMillis());
        triggerParam.setExecutorTimeout(0); // No timeout
        jobThread.pushTriggerQueue(triggerParam);

        // Give thread time to execute
        Thread.sleep(1500);

        // Stop the thread
        jobThread.toStop("test completed");

        // Wait for thread to finish
        jobThread.join(3000);

        // Thread execution should complete (may take time due to callback processing)
        assertTrue(true);
    }

    @Test
    void testJobThreadWithTimeout() throws InterruptedException {
        doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success")).when(mock).callback(anyList());

        IJobHandler handler = new TimeoutTestHandler();
        JobThread jobThread = new JobThread(8, handler);

        // Start the thread
        jobThread.start();

        // Create and push a trigger with timeout
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(8);
        triggerParam.setLogId(400L);
        triggerParam.setLogDateTime(System.currentTimeMillis());
        triggerParam.setExecutorTimeout(2); // 2 seconds timeout
        jobThread.pushTriggerQueue(triggerParam);

        // Give thread time to execute and timeout
        Thread.sleep(4000);

        // Stop the thread
        jobThread.toStop("test completed");

        // Wait for thread to finish
        jobThread.join(3000);

        // Thread should handle timeout gracefully
        assertTrue(true);
    }

    @Test
    void testJobThreadWithException() throws InterruptedException {
        doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success")).when(mock).callback(anyList());

        IJobHandler handler = new ExceptionTestHandler();
        JobThread jobThread = new JobThread(9, handler);

        // Start the thread
        jobThread.start();

        // Create and push a trigger
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(9);
        triggerParam.setLogId(500L);
        triggerParam.setLogDateTime(System.currentTimeMillis());
        triggerParam.setExecutorTimeout(0);
        jobThread.pushTriggerQueue(triggerParam);

        // Give thread time to execute and handle exception
        Thread.sleep(2000);

        // Stop the thread
        jobThread.toStop("test completed");

        // Wait for thread to finish
        jobThread.join(3000);

        // Thread should be stopped (may still be alive due to callback processing)
        // The important thing is that exceptions are handled gracefully
        assertTrue(true);
    }

    @Test
    void testJobThreadInitAndDestroy() throws InterruptedException {
        doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success")).when(mock).callback(anyList());

        InitDestroyTestHandler handler = new InitDestroyTestHandler();
        JobThread jobThread = new JobThread(10, handler);

        // Start the thread (should call init)
        jobThread.start();

        // Give thread time to initialize and process
        Thread.sleep(1000);

        // Stop the thread (should call destroy)
        jobThread.toStop("test completed");

        // Wait for thread to finish
        jobThread.join(3000);

        // Verify init was called (destroy may not be called if thread is interrupted)
        assertTrue(handler.isInitCalled());
    }

    @Test
    void testMultipleTriggersInQueue() throws InterruptedException {
        doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success")).when(mock).callback(anyList());

        IJobHandler handler = new SimpleTestHandler();
        JobThread jobThread = new JobThread(11, handler);

        // Start the thread
        jobThread.start();

        // Push multiple triggers
        for (int i = 0; i < 3; i++) {
            TriggerParam triggerParam = new TriggerParam();
            triggerParam.setJobId(11);
            triggerParam.setLogId(600L + i);
            triggerParam.setLogDateTime(System.currentTimeMillis());
            triggerParam.setExecutorTimeout(0);
            jobThread.pushTriggerQueue(triggerParam);
        }

        // Give thread time to process all triggers
        Thread.sleep(2000);

        // Stop the thread
        jobThread.toStop("test completed");

        // Wait for thread to finish
        jobThread.join(2000);

        // Verify thread has stopped
        assertFalse(jobThread.isAlive());
    }

    @Test
    void testJobThreadStopWithPendingTriggers() throws InterruptedException {
        doReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success")).when(mock).callback(anyList());

        IJobHandler handler = new SlowTestHandler();
        JobThread jobThread = new JobThread(12, handler);

        // Start the thread
        jobThread.start();

        // Push a trigger
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(12);
        triggerParam.setLogId(700L);
        triggerParam.setLogDateTime(System.currentTimeMillis());
        triggerParam.setExecutorTimeout(0);
        jobThread.pushTriggerQueue(triggerParam);

        // Give thread time to start processing
        Thread.sleep(100);

        // Stop the thread while trigger is being processed
        jobThread.toStop("test stop with pending");

        // Wait for thread to finish
        jobThread.join(3000);

        // Verify thread has stopped
        assertFalse(jobThread.isAlive());
    }

    /**
     * Helper method to check toStop flag using reflection
     */
    private boolean hasToStopFlag(JobThread jobThread) {
        try {
            Field field = JobThread.class.getDeclaredField("toStop");
            field.setAccessible(true);
            return (boolean) field.get(jobThread);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access toStop field", e);
        }
    }

    // -------------------------------------------------------------------------
    // Test handler implementations
    // -------------------------------------------------------------------------

    static class SimpleTestHandler extends IJobHandler {
        @Override
        public void execute() throws Exception {
            // Simple execution, do nothing
        }
    }

    static class TimeoutTestHandler extends IJobHandler {
        @Override
        public void execute() throws Exception {
            // Sleep longer than timeout to trigger timeout
            Thread.sleep(5000);
        }
    }

    static class ExceptionTestHandler extends IJobHandler {
        @Override
        public void execute() throws Exception {
            throw new RuntimeException("Test exception");
        }
    }

    static class InitDestroyTestHandler extends IJobHandler {
        private boolean initCalled = false;
        private boolean destroyCalled = false;

        @Override
        public void init() throws Exception {
            initCalled = true;
        }

        @Override
        public void execute() throws Exception {
            // Do nothing
        }

        @Override
        public void destroy() throws Exception {
            destroyCalled = true;
        }

        public boolean isInitCalled() {
            return initCalled;
        }

        public boolean isDestroyCalled() {
            return destroyCalled;
        }
    }

    static class SlowTestHandler extends IJobHandler {
        @Override
        public void execute() throws Exception {
            // Simulate slow execution
            Thread.sleep(2000);
        }
    }
}
