package com.xxl.job.core.handler.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.log.XxlJobFileAppender;
import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ScriptJobHandler unit test
 *
 * @author wuwen
 */
class ScriptJobHandlerTest {

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
    void testConstructorWithShellScript() {
        int jobId = 1;
        long glueUpdatetime = System.currentTimeMillis();
        String scriptContent = "#!/bin/bash\necho 'Hello World'";

        ScriptJobHandler handler = new ScriptJobHandler(jobId, glueUpdatetime, scriptContent, GlueTypeEnum.GLUE_SHELL);

        assertNotNull(handler);
        assertEquals(glueUpdatetime, handler.getGlueUpdatetime());
    }

    @Test
    void testConstructorWithPythonScript() {
        int jobId = 2;
        long glueUpdatetime = System.currentTimeMillis();
        String scriptContent = "print('Hello World')";

        ScriptJobHandler handler = new ScriptJobHandler(jobId, glueUpdatetime, scriptContent, GlueTypeEnum.GLUE_PYTHON);

        assertNotNull(handler);
        assertEquals(glueUpdatetime, handler.getGlueUpdatetime());
    }

    @Test
    void testConstructorCleansOldScripts() throws Exception {
        int jobId = 100;
        long oldTimestamp = 1000L;
        long newTimestamp = 2000L;
        String scriptContent = "echo 'test'";

        // Create old script file
        File glueSrcPath = new File(XxlJobFileAppender.getGlueSrcPath());
        glueSrcPath.mkdirs();
        File oldScript = new File(glueSrcPath, jobId + "_" + oldTimestamp + ".sh");
        oldScript.createNewFile();
        assertTrue(oldScript.exists());

        // Create new handler - should clean old script
        ScriptJobHandler handler = new ScriptJobHandler(jobId, newTimestamp, scriptContent, GlueTypeEnum.GLUE_SHELL);

        // Old script should be deleted
        assertFalse(oldScript.exists());
    }

    @Test
    void testExecuteWithInvalidGlueType() {
        int jobId = 3;
        long glueUpdatetime = System.currentTimeMillis();
        String scriptContent = "test";

        // Use BEAN type which is not a script
        ScriptJobHandler handler = new ScriptJobHandler(jobId, glueUpdatetime, scriptContent, GlueTypeEnum.BEAN);

        // Initialize job context
        initJobContext(jobId);

        // Execute should fail with invalid glue type
        assertDoesNotThrow(() -> handler.execute());
    }

    @Test
    void testGetGlueUpdatetime() {
        int jobId = 4;
        long timestamp1 = 123456789L;
        long timestamp2 = 987654321L;

        ScriptJobHandler handler1 = new ScriptJobHandler(jobId, timestamp1, "script1", GlueTypeEnum.GLUE_SHELL);
        ScriptJobHandler handler2 = new ScriptJobHandler(jobId, timestamp2, "script2", GlueTypeEnum.GLUE_SHELL);

        assertEquals(timestamp1, handler1.getGlueUpdatetime());
        assertEquals(timestamp2, handler2.getGlueUpdatetime());
    }

    @Test
    void testDifferentGlueTypes() {
        int jobId = 5;
        String scriptContent = "test";

        ScriptJobHandler shellHandler = new ScriptJobHandler(jobId, 1L, scriptContent, GlueTypeEnum.GLUE_SHELL);
        ScriptJobHandler pythonHandler = new ScriptJobHandler(jobId, 2L, scriptContent, GlueTypeEnum.GLUE_PYTHON);
        ScriptJobHandler phpHandler = new ScriptJobHandler(jobId, 3L, scriptContent, GlueTypeEnum.GLUE_PHP);
        ScriptJobHandler nodejsHandler = new ScriptJobHandler(jobId, 4L, scriptContent, GlueTypeEnum.GLUE_NODEJS);

        assertNotNull(shellHandler);
        assertNotNull(pythonHandler);
        assertNotNull(phpHandler);
        assertNotNull(nodejsHandler);
    }

    @Test
    void testConstructorWithEmptyScript() {
        int jobId = 6;
        long glueUpdatetime = System.currentTimeMillis();
        String emptyScript = "";

        ScriptJobHandler handler = new ScriptJobHandler(jobId, glueUpdatetime, emptyScript, GlueTypeEnum.GLUE_SHELL);

        assertNotNull(handler);
        assertEquals(glueUpdatetime, handler.getGlueUpdatetime());
    }

    @Test
    void testConstructorWithNullScript() {
        int jobId = 7;
        long glueUpdatetime = System.currentTimeMillis();

        ScriptJobHandler handler = new ScriptJobHandler(jobId, glueUpdatetime, null, GlueTypeEnum.GLUE_SHELL);

        assertNotNull(handler);
    }

    @Test
    void testMultipleHandlersWithSameJobId() {
        int jobId = 8;
        String scriptContent = "test";

        ScriptJobHandler handler1 = new ScriptJobHandler(jobId, 100L, scriptContent, GlueTypeEnum.GLUE_SHELL);
        ScriptJobHandler handler2 = new ScriptJobHandler(jobId, 200L, scriptContent, GlueTypeEnum.GLUE_SHELL);

        // Different handlers should have different timestamps
        assertEquals(100L, handler1.getGlueUpdatetime());
        assertEquals(200L, handler2.getGlueUpdatetime());
    }

    @Test
    void testAllScriptGlueTypes() {
        int jobId = 9;
        String scriptContent = "test";

        // Test all script types
        ScriptJobHandler shellHandler = new ScriptJobHandler(jobId, 1L, scriptContent, GlueTypeEnum.GLUE_SHELL);
        ScriptJobHandler pythonHandler = new ScriptJobHandler(jobId, 2L, scriptContent, GlueTypeEnum.GLUE_PYTHON);
        ScriptJobHandler phpHandler = new ScriptJobHandler(jobId, 3L, scriptContent, GlueTypeEnum.GLUE_PHP);
        ScriptJobHandler nodejsHandler = new ScriptJobHandler(jobId, 4L, scriptContent, GlueTypeEnum.GLUE_NODEJS);
        ScriptJobHandler powershellHandler =
                new ScriptJobHandler(jobId, 5L, scriptContent, GlueTypeEnum.GLUE_POWERSHELL);

        // All should be created successfully
        assertNotNull(shellHandler);
        assertNotNull(pythonHandler);
        assertNotNull(phpHandler);
        assertNotNull(nodejsHandler);
        assertNotNull(powershellHandler);
    }

    @Test
    void testNonScriptGlueTypes() {
        int jobId = 10;
        String content = "test";

        // Test non-script types
        ScriptJobHandler beanHandler = new ScriptJobHandler(jobId, 1L, content, GlueTypeEnum.BEAN);
        ScriptJobHandler groovyHandler = new ScriptJobHandler(jobId, 2L, content, GlueTypeEnum.GLUE_GROOVY);

        assertNotNull(beanHandler);
        assertNotNull(groovyHandler);
    }

    @Test
    void testConstructorWithLargeJobId() {
        int largeJobId = Integer.MAX_VALUE;
        long glueUpdatetime = System.currentTimeMillis();
        String scriptContent = "test";

        ScriptJobHandler handler =
                new ScriptJobHandler(largeJobId, glueUpdatetime, scriptContent, GlueTypeEnum.GLUE_SHELL);

        assertNotNull(handler);
    }

    @Test
    void testConstructorWithZeroJobId() {
        int jobId = 0;
        long glueUpdatetime = System.currentTimeMillis();
        String scriptContent = "test";

        ScriptJobHandler handler = new ScriptJobHandler(jobId, glueUpdatetime, scriptContent, GlueTypeEnum.GLUE_SHELL);

        assertNotNull(handler);
    }

    @Test
    void testConstructorWithNegativeJobId() {
        int jobId = -1;
        long glueUpdatetime = System.currentTimeMillis();
        String scriptContent = "test";

        ScriptJobHandler handler = new ScriptJobHandler(jobId, glueUpdatetime, scriptContent, GlueTypeEnum.GLUE_SHELL);

        assertNotNull(handler);
    }

    @Test
    void testGlueSrcPathCreation() throws Exception {
        int jobId = 11;
        long glueUpdatetime = System.currentTimeMillis();
        String scriptContent = "test";

        // Ensure glue source path exists
        File glueSrcPath = new File(XxlJobFileAppender.getGlueSrcPath());

        ScriptJobHandler handler = new ScriptJobHandler(jobId, glueUpdatetime, scriptContent, GlueTypeEnum.GLUE_SHELL);

        // Glue source path should exist after handler creation
        assertTrue(glueSrcPath.exists());
    }

    /**
     * Helper method to initialize job context
     */
    private void initJobContext(int jobId) {
        XxlJobContext xxlJobContext = new XxlJobContext(
                jobId, // jobId
                "test param", // jobParam
                tempLogDir.getAbsolutePath() + "/" + jobId + ".log", // logFileName
                0, // shardIndex
                1 // shardTotal
                );
        XxlJobContext.setXxlJobContext(xxlJobContext);
    }
}
