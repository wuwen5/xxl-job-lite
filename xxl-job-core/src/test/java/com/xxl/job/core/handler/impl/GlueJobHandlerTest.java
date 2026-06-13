package com.xxl.job.core.handler.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * GlueJobHandler unit test
 *
 * @author wuwen
 */
class GlueJobHandlerTest {

    @TempDir
    static File tempLogDir;

    @BeforeAll
    static void setUp() {
        // Set temp directory as log path for testing
        XxlJobFileAppender.initLogPath(tempLogDir.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {}

    @Test
    void testConstructor() {
        IJobHandler mockHandler = mock(IJobHandler.class);
        long glueUpdatetime = System.currentTimeMillis();

        GlueJobHandler handler = new GlueJobHandler(mockHandler, glueUpdatetime);

        assertNotNull(handler);
        assertEquals(glueUpdatetime, handler.getGlueUpdatetime());
    }

    @Test
    void testGetGlueUpdatetime() {
        IJobHandler mockHandler = mock(IJobHandler.class);
        long timestamp1 = 1000L;
        long timestamp2 = 2000L;

        GlueJobHandler handler1 = new GlueJobHandler(mockHandler, timestamp1);
        GlueJobHandler handler2 = new GlueJobHandler(mockHandler, timestamp2);

        assertEquals(timestamp1, handler1.getGlueUpdatetime());
        assertEquals(timestamp2, handler2.getGlueUpdatetime());
    }

    @Test
    void testExecute() throws Exception {
        IJobHandler mockHandler = mock(IJobHandler.class);
        GlueJobHandler handler = new GlueJobHandler(mockHandler, 123456789L);

        // Initialize job context
        initJobContext();

        // Execute
        handler.execute();

        // Verify inner handler was called
        verify(mockHandler, times(1)).execute();
    }

    @Test
    void testExecuteWithException() throws Exception {
        IJobHandler mockHandler = mock(IJobHandler.class);
        doThrow(new RuntimeException("Test exception")).when(mockHandler).execute();

        GlueJobHandler handler = new GlueJobHandler(mockHandler, 123456789L);

        // Initialize job context
        initJobContext();

        // Execute should propagate exception
        assertThrows(RuntimeException.class, () -> handler.execute());

        // Verify inner handler was called
        verify(mockHandler, times(1)).execute();
    }

    @Test
    void testInit() throws Exception {
        IJobHandler mockHandler = mock(IJobHandler.class);
        GlueJobHandler handler = new GlueJobHandler(mockHandler, 123456789L);

        // Call init
        handler.init();

        // Verify inner handler init was called
        verify(mockHandler, times(1)).init();
    }

    @Test
    void testInitWithException() throws Exception {
        IJobHandler mockHandler = mock(IJobHandler.class);
        doThrow(new RuntimeException("Init exception")).when(mockHandler).init();

        GlueJobHandler handler = new GlueJobHandler(mockHandler, 123456789L);

        // Init should propagate exception
        assertThrows(RuntimeException.class, () -> handler.init());

        // Verify inner handler init was called
        verify(mockHandler, times(1)).init();
    }

    @Test
    void testDestroy() throws Exception {
        IJobHandler mockHandler = mock(IJobHandler.class);
        GlueJobHandler handler = new GlueJobHandler(mockHandler, 123456789L);

        // Call destroy
        handler.destroy();

        // Verify inner handler destroy was called
        verify(mockHandler, times(1)).destroy();
    }

    @Test
    void testDestroyWithException() throws Exception {
        IJobHandler mockHandler = mock(IJobHandler.class);
        doThrow(new RuntimeException("Destroy exception")).when(mockHandler).destroy();

        GlueJobHandler handler = new GlueJobHandler(mockHandler, 123456789L);

        // Destroy should propagate exception
        assertThrows(RuntimeException.class, () -> handler.destroy());

        // Verify inner handler destroy was called
        verify(mockHandler, times(1)).destroy();
    }

    @Test
    void testFullLifecycle() throws Exception {
        IJobHandler mockHandler = mock(IJobHandler.class);
        GlueJobHandler handler = new GlueJobHandler(mockHandler, 999L);

        // Initialize job context
        initJobContext();

        // Full lifecycle: init -> execute -> destroy
        handler.init();
        handler.execute();
        handler.destroy();

        // Verify all methods were called
        verify(mockHandler, times(1)).init();
        verify(mockHandler, times(1)).execute();
        verify(mockHandler, times(1)).destroy();
    }

    @Test
    void testMultipleExecutes() throws Exception {
        IJobHandler mockHandler = mock(IJobHandler.class);
        GlueJobHandler handler = new GlueJobHandler(mockHandler, 888L);

        // Initialize job context
        initJobContext();

        // Execute multiple times
        handler.execute();
        handler.execute();
        handler.execute();

        // Verify inner handler was called 3 times
        verify(mockHandler, times(3)).execute();
    }

    @Test
    void testDifferentGlueVersions() throws Exception {
        IJobHandler mockHandler1 = mock(IJobHandler.class);
        IJobHandler mockHandler2 = mock(IJobHandler.class);

        GlueJobHandler handler1 = new GlueJobHandler(mockHandler1, 100L);
        GlueJobHandler handler2 = new GlueJobHandler(mockHandler2, 200L);

        assertEquals(100L, handler1.getGlueUpdatetime());
        assertEquals(200L, handler2.getGlueUpdatetime());

        // Different handlers should be independent
        assertNotSame(handler1, handler2);
    }

    /**
     * Helper method to initialize job context
     */
    private void initJobContext() {
        XxlJobContext xxlJobContext = new XxlJobContext(
                1, // jobId
                "test param", // jobParam
                tempLogDir.getAbsolutePath() + "/test.log", // logFileName
                0, // shardIndex
                1 // shardTotal
                );
        XxlJobContext.setXxlJobContext(xxlJobContext);
    }
}
