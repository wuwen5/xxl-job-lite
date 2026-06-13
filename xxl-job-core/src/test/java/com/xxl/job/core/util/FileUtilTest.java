package com.xxl.job.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FileUtil unit test
 *
 * @author wuwen
 */
class FileUtilTest {

    @TempDir
    File tempDir;

    @Test
    void testDeleteRecursivelyWithFile() throws IOException {
        File file = new File(tempDir, "test.txt");
        file.createNewFile();

        boolean result = FileUtil.deleteRecursively(file);
        assertTrue(result);
        assertFalse(file.exists());
    }

    @Test
    void testDeleteRecursivelyWithDirectory() throws IOException {
        File dir = new File(tempDir, "testdir");
        dir.mkdirs();

        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        boolean result = FileUtil.deleteRecursively(dir);
        assertTrue(result);
        assertFalse(dir.exists());
        assertFalse(file1.exists());
        assertFalse(file2.exists());
    }

    @Test
    void testDeleteRecursivelyWithNestedDirectory() throws IOException {
        File parentDir = new File(tempDir, "parent");
        File childDir = new File(parentDir, "child");
        childDir.mkdirs();

        File file = new File(childDir, "nested.txt");
        file.createNewFile();

        boolean result = FileUtil.deleteRecursively(parentDir);
        assertTrue(result);
        assertFalse(parentDir.exists());
        assertFalse(childDir.exists());
        assertFalse(file.exists());
    }

    @Test
    void testDeleteRecursivelyWithNull() {
        boolean result = FileUtil.deleteRecursively(null);
        assertFalse(result);
    }

    @Test
    void testDeleteRecursivelyWithNonExistentFile() {
        File file = new File(tempDir, "nonexistent.txt");
        boolean result = FileUtil.deleteRecursively(file);
        assertFalse(result);
    }

    @Test
    void testDeleteRecursivelyWithEmptyDirectory() throws IOException {
        File dir = new File(tempDir, "emptydir");
        dir.mkdirs();

        boolean result = FileUtil.deleteRecursively(dir);
        assertTrue(result);
        assertFalse(dir.exists());
    }

    @Test
    void testWriteAndReadFileContent() throws IOException {
        File file = new File(tempDir, "test.txt");
        String content = "Hello, World!";
        byte[] data = content.getBytes(StandardCharsets.UTF_8);

        // Write content
        FileUtil.writeFileContent(file, data);

        // Verify file exists
        assertTrue(file.exists());

        // Read content
        byte[] readData = FileUtil.readFileContent(file);

        assertNotNull(readData);
        assertEquals(content.length(), readData.length);
        assertEquals(content, new String(readData, StandardCharsets.UTF_8));
    }

    @Test
    void testWriteFileContentCreatesParentDirs() throws IOException {
        File nestedDir = new File(tempDir, "a/b/c");
        File file = new File(nestedDir, "test.txt");

        String content = "Nested content";
        byte[] data = content.getBytes(StandardCharsets.UTF_8);

        FileUtil.writeFileContent(file, data);

        assertTrue(file.exists());
        assertTrue(nestedDir.exists());
    }

    @Test
    void testWriteFileContentOverwrite() throws IOException {
        File file = new File(tempDir, "overwrite.txt");

        // Write first content
        FileUtil.writeFileContent(file, "First".getBytes(StandardCharsets.UTF_8));

        // Write second content (should overwrite)
        FileUtil.writeFileContent(file, "Second".getBytes(StandardCharsets.UTF_8));

        // Read and verify
        byte[] readData = FileUtil.readFileContent(file);
        String content = new String(readData, StandardCharsets.UTF_8);

        assertEquals("Second", content);
    }

    @Test
    void testReadFileContentWithBinaryData() throws IOException {
        File file = new File(tempDir, "binary.dat");

        // Create binary data
        byte[] data = new byte[256];
        for (int i = 0; i < 256; i++) {
            data[i] = (byte) i;
        }

        // Write and read
        FileUtil.writeFileContent(file, data);
        byte[] readData = FileUtil.readFileContent(file);

        assertNotNull(readData);
        assertArrayEquals(data, readData);
    }

    @Test
    void testReadFileContentWithLargeFile() throws IOException {
        File file = new File(tempDir, "large.txt");

        // Create large content (1MB)
        byte[] data = new byte[1024 * 1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }

        FileUtil.writeFileContent(file, data);
        byte[] readData = FileUtil.readFileContent(file);

        assertNotNull(readData);
        assertEquals(data.length, readData.length);
    }

    @Test
    void testWriteFileContentWithEmptyData() throws IOException {
        File file = new File(tempDir, "empty.txt");
        byte[] data = new byte[0];

        FileUtil.writeFileContent(file, data);

        assertTrue(file.exists());
        assertEquals(0, file.length());
    }

    @Test
    void testReadFileContentWithEmptyFile() throws IOException {
        File file = new File(tempDir, "empty.txt");
        file.createNewFile();

        byte[] readData = FileUtil.readFileContent(file);

        assertNotNull(readData);
        assertEquals(0, readData.length);
    }

    @Test
    void testRoundTripWriteAndRead() throws IOException {
        File file = new File(tempDir, "roundtrip.txt");
        String originalContent = "This is a test content with special chars: @#$%^&*()";
        byte[] originalData = originalContent.getBytes(StandardCharsets.UTF_8);

        // Write
        FileUtil.writeFileContent(file, originalData);

        // Read
        byte[] readData = FileUtil.readFileContent(file);
        String readContent = new String(readData, StandardCharsets.UTF_8);

        assertEquals(originalContent, readContent);
    }

    @Test
    void testDeleteRecursivelyWithMultipleLevels() throws IOException {
        File root = new File(tempDir, "root");
        File level1 = new File(root, "level1");
        File level2 = new File(level1, "level2");
        File level3 = new File(level2, "level3");
        level3.mkdirs();

        // Create files at different levels
        File file1 = new File(root, "file1.txt");
        File file2 = new File(level1, "file2.txt");
        File file3 = new File(level2, "file3.txt");
        File file4 = new File(level3, "file4.txt");

        file1.createNewFile();
        file2.createNewFile();
        file3.createNewFile();
        file4.createNewFile();

        // Delete recursively from root
        boolean result = FileUtil.deleteRecursively(root);

        assertTrue(result);
        assertFalse(root.exists());
        assertFalse(level1.exists());
        assertFalse(level2.exists());
        assertFalse(level3.exists());
        assertFalse(file1.exists());
        assertFalse(file2.exists());
        assertFalse(file3.exists());
        assertFalse(file4.exists());
    }
}
