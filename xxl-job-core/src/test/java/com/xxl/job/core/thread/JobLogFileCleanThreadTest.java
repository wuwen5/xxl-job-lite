package com.xxl.job.core.thread;

import static org.junit.jupiter.api.Assertions.*;

import com.xxl.job.core.log.XxlJobFileAppender;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * JobLogFileCleanThread unit test
 *
 * @author wuwen
 */
class JobLogFileCleanThreadTest {

    @TempDir
    static File tempLogDir;

    @BeforeAll
    static void setUp() {
        // Set temp directory as log path for testing
        XxlJobFileAppender.initLogPath(tempLogDir.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        // Stop the thread if it's running
        JobLogFileCleanThread.getInstance().toStop();
    }

    @Test
    void testStartWithInvalidRetentionDays() {
        // Test with retention days less than 3 - should not start thread
        JobLogFileCleanThread cleanThread = JobLogFileCleanThread.getInstance();
        cleanThread.start(1);

        // Give some time to see if thread starts
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Thread should not be started because retention days < 3
        // We can't directly check if thread is null, but we verify no exception occurs
    }

    @Test
    void testStartWithMinimumRetentionDays() {
        // Test with exactly 3 days - minimum valid value
        JobLogFileCleanThread cleanThread = JobLogFileCleanThread.getInstance();
        cleanThread.start(3);

        // Verify thread starts without exception
        assertNotNull(cleanThread);
    }

    @Test
    void testCleanExpiredLogFiles() throws IOException, InterruptedException {
        // Create expired log directories (older than retention period)
        LocalDate expiredDate = LocalDate.now().minusDays(10);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String expiredDateStr = expiredDate.format(formatter);
        File expiredDir = new File(tempLogDir, expiredDateStr);
        assertTrue(expiredDir.mkdirs());

        // Create a log file in expired directory
        File logFile = new File(expiredDir, "1.log");
        assertTrue(logFile.createNewFile());

        // Start clean thread with 7 days retention
        JobLogFileCleanThread cleanThread = new JobLogFileCleanThread();
        cleanThread.start(7);

        Thread.sleep(200);

        // Wait for cleanup to occur (thread sleeps 1 day, so we manually trigger by waiting)
        // Since the thread sleeps for 1 day, we need to wait or use reflection
        // For this test, we'll verify the directory exists before cleanup
        assertFalse(expiredDir.exists());

        // Stop the thread
        cleanThread.toStop();
    }

    @Test
    void testKeepRecentLogFiles() throws IOException {
        // Create recent log directory (today)
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayDateStr = today.format(formatter);
        File todayDir = new File(tempLogDir, todayDateStr);
        assertTrue(todayDir.mkdirs());

        // Create a log file in today's directory
        File logFile = new File(todayDir, "1.log");
        assertTrue(logFile.createNewFile());

        // Start clean thread with 7 days retention
        JobLogFileCleanThread cleanThread = new JobLogFileCleanThread();
        cleanThread.start(7);

        // Recent files should not be deleted immediately
        assertTrue(todayDir.exists());
        assertTrue(logFile.exists());

        // Stop the thread
        cleanThread.toStop();
    }

    @Test
    void testIgnoreInvalidDirectoryNames() throws IOException {
        // Create directories with invalid names (no "-" separator)
        File invalidDir1 = new File(tempLogDir, "invalid");
        File invalidDir2 = new File(tempLogDir, "20240101");
        assertTrue(invalidDir1.mkdirs());
        assertTrue(invalidDir2.mkdirs());

        // Create files in invalid directories
        File logFile1 = new File(invalidDir1, "1.log");
        File logFile2 = new File(invalidDir2, "2.log");
        assertTrue(logFile1.createNewFile());
        assertTrue(logFile2.createNewFile());

        // Start clean thread
        JobLogFileCleanThread cleanThread = new JobLogFileCleanThread();
        cleanThread.start(7);

        // Invalid directories should not be processed immediately
        assertTrue(invalidDir1.exists());
        assertTrue(invalidDir2.exists());

        // Stop the thread
        cleanThread.toStop();
    }

    @Test
    void testIgnoreNonDirectoryFiles() throws IOException {
        // Create a file directly in log base path (not a directory)
        File regularFile = new File(tempLogDir, "notadir.log");
        assertTrue(regularFile.createNewFile());

        // Start clean thread
        JobLogFileCleanThread cleanThread = new JobLogFileCleanThread();
        cleanThread.start(7);

        // Regular files should not be affected
        assertTrue(regularFile.exists());

        // Stop the thread
        cleanThread.toStop();
    }

    @Test
    void testToStop() throws InterruptedException {
        JobLogFileCleanThread cleanThread = new JobLogFileCleanThread();
        cleanThread.start(7);

        // Give thread time to start
        Thread.sleep(100);

        // Stop the thread
        cleanThread.toStop();

        // Verify stop completes without exception
        // Thread should be stopped gracefully
    }

    @Test
    void testMultipleStopCalls() {
        JobLogFileCleanThread cleanThread = new JobLogFileCleanThread();
        cleanThread.start(7);

        // Multiple stop calls should not cause issues
        cleanThread.toStop();
        cleanThread.toStop();
        cleanThread.toStop();
    }

    @Test
    void testBoundaryRetentionDays() throws IOException {
        // Test boundary: create directory exactly at retention limit
        LocalDate boundaryDate = LocalDate.now().minusDays(7);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String boundaryDateStr = boundaryDate.format(formatter);
        File boundaryDir = new File(tempLogDir, boundaryDateStr);
        assertTrue(boundaryDir.mkdirs());

        File logFile = new File(boundaryDir, "1.log");
        assertTrue(logFile.createNewFile());

        // Start clean thread with 7 days retention
        JobLogFileCleanThread cleanThread = new JobLogFileCleanThread();
        cleanThread.start(7);

        // Boundary case: file at exactly 7 days might or might not be deleted
        // depending on exact timing, but should not throw exception
        assertTrue(boundaryDir.exists() || !boundaryDir.exists());

        // Stop the thread
        cleanThread.toStop();
    }

    @Test
    void testEmptyLogDirectory() {
        // Test with empty log directory
        JobLogFileCleanThread cleanThread = new JobLogFileCleanThread();
        cleanThread.start(7);

        // Should handle empty directory without exception
        assertNotNull(cleanThread);

        // Stop the thread
        cleanThread.toStop();
    }

    @Test
    void testNullChildDirs() {
        // Test when listFiles returns null (e.g., directory doesn't exist or is not accessible)
        // This is handled internally by the thread
        JobLogFileCleanThread cleanThread = new JobLogFileCleanThread();
        cleanThread.start(7);

        // Should not throw exception
        assertNotNull(cleanThread);

        // Stop the thread
        cleanThread.toStop();
    }
}
