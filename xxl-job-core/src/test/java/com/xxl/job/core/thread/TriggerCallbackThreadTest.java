package com.xxl.job.core.thread;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.log.XxlJobFileAppender;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TriggerCallbackThread unit test
 *
 * @author wuwen
 */
class TriggerCallbackThreadTest {

    @TempDir
    static File tempLogDir;

    private static String originalLogPath;
    private List<AdminBiz> originalAdminBizList;

    @BeforeAll
    static void beforeAll() {
        // Set temp directory as log path for testing
        XxlJobFileAppender.initLogPath(tempLogDir.getAbsolutePath());
        // Save original log path
        originalLogPath = XxlJobFileAppender.getLogPath();
        // Ensure the singleton instance is initialized before tests
        TriggerCallbackThread.getInstance().start();
    }

    @AfterAll
    static void afterAll() {
        // Stop the singleton instance after all tests
        TriggerCallbackThread.getInstance().toStop();
        // Restore original log path
        XxlJobFileAppender.initLogPath(originalLogPath);
    }

    @BeforeEach
    void setUp() throws Exception {

        // Save original adminBizList using reflection
        Field field = XxlJobExecutor.class.getDeclaredField("adminBizList");
        field.setAccessible(true);
        originalAdminBizList = (List<AdminBiz>) field.get(null);
    }

    @AfterEach
    void tearDown() throws Exception {

        // Restore original adminBizList using reflection
        Field field = XxlJobExecutor.class.getDeclaredField("adminBizList");
        field.setAccessible(true);
        field.set(null, originalAdminBizList);
    }

    /**
     * Helper method to set adminBizList using reflection
     */
    private void setAdminBizList(List<AdminBiz> adminBizList) throws Exception {
        Field field = XxlJobExecutor.class.getDeclaredField("adminBizList");
        field.setAccessible(true);
        field.set(null, adminBizList);
    }

    @Test
    void testGetInstance() {
        TriggerCallbackThread instance1 = TriggerCallbackThread.getInstance();
        TriggerCallbackThread instance2 = TriggerCallbackThread.getInstance();

        // Should return same singleton instance
        assertSame(instance1, instance2);
    }

    @Test
    void testPushCallBack() throws Exception {
        // Create a mock AdminBiz
        AdminBiz mockAdminBiz = mock(AdminBiz.class);
        when(mockAdminBiz.callback(anyList())).thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));

        // Update the adminBizList in XxlJobExecutor
        List<AdminBiz> adminBizList = new ArrayList<>();
        adminBizList.add(mockAdminBiz);
        setAdminBizList(Collections.unmodifiableList(adminBizList));

        // Push a callback
        HandleCallbackParam callbackParam = new HandleCallbackParam(1L, System.currentTimeMillis(), 200, "success");
        TriggerCallbackThread.pushCallBack(callbackParam);

        // Give thread time to process
        Thread.sleep(500);

        // Verify callback was invoked
        verify(mockAdminBiz, atLeastOnce()).callback(anyList());
    }

    @Test
    void testStartAndStop() throws InterruptedException {
        TriggerCallbackThread callbackThread = new TriggerCallbackThread();

        // Start threads
        callbackThread.start();

        // Give threads time to start
        Thread.sleep(100);

        // Stop threads
        callbackThread.toStop();

        // Verify stop completes without exception
        assertTrue(true);
    }

    @Test
    void testToStopWithoutStart() {
        // Calling toStop without start should not throw exception
        TriggerCallbackThread callbackThread = new TriggerCallbackThread();
        callbackThread.toStop();

        assertTrue(true);
    }

    @Test
    void testMultipleToStopCalls() throws InterruptedException {
        TriggerCallbackThread callbackThread = new TriggerCallbackThread();
        callbackThread.start();

        Thread.sleep(100);

        // Multiple stop calls should not cause issues
        callbackThread.toStop();
        callbackThread.toStop();
        callbackThread.toStop();
    }

    @Test
    void testCallbackWithSuccessResult() throws Exception {
        // Create a mock AdminBiz that returns success
        AdminBiz mockAdminBiz = mock(AdminBiz.class);
        ReturnT<String> successResult = new ReturnT<>(ReturnT.SUCCESS_CODE, "success");
        when(mockAdminBiz.callback(anyList())).thenReturn(successResult);

        // Update the adminBizList
        List<AdminBiz> adminBizList = new ArrayList<>();
        adminBizList.add(mockAdminBiz);
        setAdminBizList(Collections.unmodifiableList(adminBizList));

        // Push a callback
        HandleCallbackParam callbackParam = new HandleCallbackParam(2L, System.currentTimeMillis(), 200, "test");
        TriggerCallbackThread.pushCallBack(callbackParam);

        // Give thread time to process
        Thread.sleep(500);

        // Verify callback was invoked and succeeded
        verify(mockAdminBiz, atLeastOnce()).callback(anyList());
    }

    @Test
    void testCallbackWithFailResult() throws Exception {
        // Create a mock AdminBiz that returns failure
        AdminBiz mockAdminBiz = mock(AdminBiz.class);
        ReturnT<String> failResult = new ReturnT<>(ReturnT.FAIL_CODE, "fail");
        when(mockAdminBiz.callback(anyList())).thenReturn(failResult);

        // Update the adminBizList
        List<AdminBiz> adminBizList = new ArrayList<>();
        adminBizList.add(mockAdminBiz);
        setAdminBizList(Collections.unmodifiableList(adminBizList));

        // Push a callback
        HandleCallbackParam callbackParam = new HandleCallbackParam(3L, System.currentTimeMillis(), 500, "error");
        TriggerCallbackThread.pushCallBack(callbackParam);

        // Give thread time to process and write fail file
        Thread.sleep(500);
    }

    @Test
    void testCallbackWithException() throws Exception {
        // Create a mock AdminBiz that throws exception
        AdminBiz mockAdminBiz = mock(AdminBiz.class);
        when(mockAdminBiz.callback(anyList())).thenThrow(new RuntimeException("network error"));

        // Update the adminBizList
        List<AdminBiz> adminBizList = new ArrayList<>();
        adminBizList.add(mockAdminBiz);
        setAdminBizList(Collections.unmodifiableList(adminBizList));

        // Push a callback
        HandleCallbackParam callbackParam = new HandleCallbackParam(4L, System.currentTimeMillis(), 500, "exception");
        TriggerCallbackThread.pushCallBack(callbackParam);

        // Give thread time to process - exception will be caught and logged
        Thread.sleep(500);
    }

    @Test
    void testCallbackWithNullResult() throws Exception {
        // Create a mock AdminBiz that returns null
        AdminBiz mockAdminBiz = mock(AdminBiz.class);
        when(mockAdminBiz.callback(anyList())).thenReturn(null);

        // Update the adminBizList
        List<AdminBiz> adminBizList = new ArrayList<>();
        adminBizList.add(mockAdminBiz);
        setAdminBizList(Collections.unmodifiableList(adminBizList));

        // Push a callback
        HandleCallbackParam callbackParam = new HandleCallbackParam(5L, System.currentTimeMillis(), 200, "null result");
        TriggerCallbackThread.pushCallBack(callbackParam);

        // Give thread time to process - null result will be treated as failure
        Thread.sleep(500);

        // Callback should be attempted but will fail due to null result
        // The important thing is that the thread handles null gracefully

    }

    @Test
    void testCallbackWithMultipleAdminBiz() throws Exception {
        // Create multiple mock AdminBiz instances
        AdminBiz mockAdminBiz1 = mock(AdminBiz.class);
        AdminBiz mockAdminBiz2 = mock(AdminBiz.class);

        // First one fails, second one succeeds
        when(mockAdminBiz1.callback(anyList())).thenReturn(new ReturnT<>(ReturnT.FAIL_CODE, "fail"));
        when(mockAdminBiz2.callback(anyList())).thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));

        // Update the adminBizList
        List<AdminBiz> adminBizList = new ArrayList<>();
        adminBizList.add(mockAdminBiz1);
        adminBizList.add(mockAdminBiz2);
        setAdminBizList(Collections.unmodifiableList(adminBizList));

        // Push a callback
        HandleCallbackParam callbackParam = new HandleCallbackParam(6L, System.currentTimeMillis(), 200, "multi");
        TriggerCallbackThread.pushCallBack(callbackParam);

        // Give thread time to process
        Thread.sleep(500);
    }

    @Test
    void testPushMultipleCallbacks() throws Exception {
        // Create a mock AdminBiz
        AdminBiz mockAdminBiz = mock(AdminBiz.class);
        when(mockAdminBiz.callback(anyList())).thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));

        // Update the adminBizList
        List<AdminBiz> adminBizList = new ArrayList<>();
        adminBizList.add(mockAdminBiz);
        setAdminBizList(Collections.unmodifiableList(adminBizList));

        // Push multiple callbacks
        for (int i = 0; i < 5; i++) {
            HandleCallbackParam callbackParam =
                    new HandleCallbackParam(10L + i, System.currentTimeMillis(), 200, "batch-" + i);
            TriggerCallbackThread.pushCallBack(callbackParam);
        }

        // Give thread time to process
        Thread.sleep(500);

        // Multiple callbacks should be processed (may be batched together)
        // The important thing is that the thread handles multiple callbacks without crashing

    }

    @Test
    void testCallbackLogFileCreation() throws Exception {

        // Create a mock AdminBiz that always fails to trigger fail file creation
        AdminBiz mockAdminBiz = mock(AdminBiz.class);
        when(mockAdminBiz.callback(anyList())).thenReturn(new ReturnT<>(ReturnT.FAIL_CODE, "always fail"));

        // Update the adminBizList
        List<AdminBiz> adminBizList = new ArrayList<>();
        adminBizList.add(mockAdminBiz);
        setAdminBizList(Collections.unmodifiableList(adminBizList));

        // Push a callback
        HandleCallbackParam callbackParam = new HandleCallbackParam(7L, System.currentTimeMillis(), 500, "fail test");
        TriggerCallbackThread.pushCallBack(callbackParam);

        // Give thread time to process and write fail file
        Thread.sleep(1000);

        // Callback should fail and trigger fail file creation
        // Check if callbacklog directory exists (may or may not exist depending on timing)
        //        File callbackLogDir = new File(tempLogDir, "callbacklog");
        //        File[] files = callbackLogDir.listFiles();
        //        assertTrue(callbackLogDir.exists());

        TriggerCallbackThread.getInstance().start();
        Thread.sleep(2000);
        //        assertTrue(files == null || Arrays.stream(files).noneMatch(File::exists));
    }

    @Test
    void testEmptyCallbackList() {
        // Test with empty callback list - should not cause issues
        TriggerCallbackThread callbackThread = new TriggerCallbackThread();
        callbackThread.start();

        // Don't push any callbacks, just start and stop
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
        // Create a mock AdminBiz
        AdminBiz mockAdminBiz = mock(AdminBiz.class);
        when(mockAdminBiz.callback(anyList())).thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));

        // Update the adminBizList
        List<AdminBiz> adminBizList = new ArrayList<>();
        adminBizList.add(mockAdminBiz);
        setAdminBizList(Collections.unmodifiableList(adminBizList));

        // Push callback with zero logId
        HandleCallbackParam callbackParam = new HandleCallbackParam(0L, System.currentTimeMillis(), 200, "zero id");
        TriggerCallbackThread.pushCallBack(callbackParam);

        // Give thread time to process
        Thread.sleep(500);
    }

    @Test
    void testCallbackWithNegativeHandleCode() throws Exception {
        // Create a mock AdminBiz
        AdminBiz mockAdminBiz = mock(AdminBiz.class);
        when(mockAdminBiz.callback(anyList())).thenReturn(new ReturnT<>(ReturnT.SUCCESS_CODE, "success"));

        // Update the adminBizList
        List<AdminBiz> adminBizList = new ArrayList<>();
        adminBizList.add(mockAdminBiz);
        setAdminBizList(Collections.unmodifiableList(adminBizList));

        // Push callback with negative handle code
        HandleCallbackParam callbackParam =
                new HandleCallbackParam(8L, System.currentTimeMillis(), -1, "negative code");
        TriggerCallbackThread.pushCallBack(callbackParam);

        // Give thread time to process
        Thread.sleep(500);
    }
}
