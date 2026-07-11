package com.xxl.job.core.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ScriptUtil unit test
 *
 * @author wuwen
 */
class ScriptUtilTest {

    @TempDir
    File tempDir;

    @Test
    void testMarkScriptFile() throws IOException {
        File scriptFile = new File(tempDir, "test.sh");
        String content = "echo 'hello world'";

        ScriptUtil.markScriptFile(scriptFile.getAbsolutePath(), content);

        assertTrue(scriptFile.exists());
        byte[] bytes = Files.readAllBytes(scriptFile.toPath());
        assertEquals(content, new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void testMarkScriptFileWithEmptyContent() throws IOException {
        File scriptFile = new File(tempDir, "empty.sh");

        ScriptUtil.markScriptFile(scriptFile.getAbsolutePath(), "");

        assertTrue(scriptFile.exists());
        assertEquals(0, scriptFile.length());
    }

    @Test
    void testMarkScriptFileWithUnicodeContent() throws IOException {
        File scriptFile = new File(tempDir, "unicode.sh");
        String content = "echo '你好，世界 🌍'";

        ScriptUtil.markScriptFile(scriptFile.getAbsolutePath(), content);

        byte[] bytes = Files.readAllBytes(scriptFile.toPath());
        assertEquals(content, new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void testExecToFileWithSuccess() throws IOException {
        assumeTrue(!isWindows(), "Skipped on Windows");

        File scriptFile = new File(tempDir, "success.sh");
        File logFile = new File(tempDir, "success.log");
        String content = "echo 'stdout message'";
        ScriptUtil.markScriptFile(scriptFile.getAbsolutePath(), content);

        int exitValue = ScriptUtil.execToFile("/bin/sh", scriptFile.getAbsolutePath(), logFile.getAbsolutePath());

        assertEquals(0, exitValue);
        assertTrue(logFile.exists());
        String logContent = new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(logContent.contains("stdout message"));
    }

    @Test
    void testExecToFileWithErrorOutput() throws IOException {
        assumeTrue(!isWindows(), "Skipped on Windows");

        File scriptFile = new File(tempDir, "error.sh");
        File logFile = new File(tempDir, "error.log");
        String content = "echo 'stderr message' >&2";
        ScriptUtil.markScriptFile(scriptFile.getAbsolutePath(), content);

        int exitValue = ScriptUtil.execToFile("/bin/sh", scriptFile.getAbsolutePath(), logFile.getAbsolutePath());

        assertEquals(0, exitValue);
        assertTrue(logFile.exists());
        String logContent = new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(logContent.contains("stderr message"));
    }

    @Test
    void testExecToFileWithParams() throws IOException {
        assumeTrue(!isWindows(), "Skipped on Windows");

        File scriptFile = new File(tempDir, "params.sh");
        File logFile = new File(tempDir, "params.log");
        String content = "echo \"param1=$1, param2=$2\"";
        ScriptUtil.markScriptFile(scriptFile.getAbsolutePath(), content);

        int exitValue = ScriptUtil.execToFile(
                "/bin/sh", scriptFile.getAbsolutePath(), logFile.getAbsolutePath(), "hello", "world");

        assertEquals(0, exitValue);
        String logContent = new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(logContent.contains("param1=hello"));
        assertTrue(logContent.contains("param2=world"));
    }

    @Test
    void testExecToFileWithNonExistentCommand() throws IOException {
        File scriptFile = new File(tempDir, "not_used.sh");
        File logFile = new File(tempDir, "not_used.log");
        ScriptUtil.markScriptFile(scriptFile.getAbsolutePath(), "echo 'not used'");

        int exitValue = ScriptUtil.execToFile(
                "/non_existent_command_xyz", scriptFile.getAbsolutePath(), logFile.getAbsolutePath());

        assertNotEquals(0, exitValue);
    }

    @Test
    void testExecToFileWithFailingScript() throws IOException {
        assumeTrue(!isWindows(), "Skipped on Windows");

        File scriptFile = new File(tempDir, "fail.sh");
        File logFile = new File(tempDir, "fail.log");
        String content = "exit 42";
        ScriptUtil.markScriptFile(scriptFile.getAbsolutePath(), content);

        int exitValue = ScriptUtil.execToFile("/bin/sh", scriptFile.getAbsolutePath(), logFile.getAbsolutePath());

        assertEquals(42, exitValue);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }
}
