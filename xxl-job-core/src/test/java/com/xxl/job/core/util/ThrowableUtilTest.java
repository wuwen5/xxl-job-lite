package com.xxl.job.core.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * ThrowableUtil unit test
 *
 * @author wuwen
 */
class ThrowableUtilTest {

    @Test
    void testToStringWithException() {
        Exception e = new RuntimeException("Test exception");
        String result = ThrowableUtil.toString(e);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("RuntimeException"));
        assertTrue(result.contains("Test exception"));
    }

    @Test
    void testToStringWithNestedException() {
        Exception cause = new IllegalArgumentException("Cause exception");
        Exception e = new RuntimeException("Outer exception", cause);

        String result = ThrowableUtil.toString(e);

        assertNotNull(result);
        assertTrue(result.contains("RuntimeException"));
        assertTrue(result.contains("Outer exception"));
        assertTrue(result.contains("IllegalArgumentException"));
        assertTrue(result.contains("Cause exception"));
    }

    @Test
    void testToStringContainsStackTrace() {
        Exception e = new NullPointerException("NPE test");
        String result = ThrowableUtil.toString(e);

        // Should contain stack trace information
        assertTrue(result.contains("at "));
        assertTrue(result.contains("ThrowableUtilTest"));
    }

    @Test
    void testToStringWithCustomException() {
        CustomException e = new CustomException("Custom error message");
        String result = ThrowableUtil.toString(e);

        assertNotNull(result);
        assertTrue(result.contains("CustomException"));
        assertTrue(result.contains("Custom error message"));
    }

    @Test
    void testToStringWithDeeplyNestedException() {
        Exception level3 = new IllegalStateException("Level 3");
        Exception level2 = new RuntimeException("Level 2", level3);
        Exception level1 = new Exception("Level 1", level2);

        String result = ThrowableUtil.toString(level1);

        assertNotNull(result);
        assertTrue(result.contains("Level 1"));
        assertTrue(result.contains("Level 2"));
        assertTrue(result.contains("Level 3"));
        assertTrue(result.contains("IllegalStateException"));
    }

    @Test
    void testToStringFormat() {
        Exception e = new ArithmeticException("Divide by zero");
        String result = ThrowableUtil.toString(e);

        // Should start with exception class name and message
        assertTrue(result.startsWith("java.lang.ArithmeticException: Divide by zero"));
    }

    @Test
    void testToStringWithNullMessage() {
        Exception e = new NullPointerException();
        String result = ThrowableUtil.toString(e);

        assertNotNull(result);
        assertTrue(result.contains("NullPointerException"));
    }

    @Test
    void testToStringMultipleTimes() {
        Exception e = new RuntimeException("Repeated test");

        String result1 = ThrowableUtil.toString(e);
        String result2 = ThrowableUtil.toString(e);

        // Both should produce similar output (stack traces may differ slightly)
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.contains("Repeated test"));
        assertTrue(result2.contains("Repeated test"));
    }

    @Test
    void testToStringWithCheckedException() {
        try {
            throw new java.io.IOException("IO error");
        } catch (Exception e) {
            String result = ThrowableUtil.toString(e);

            assertNotNull(result);
            assertTrue(result.contains("IOException"));
            assertTrue(result.contains("IO error"));
        }
    }

    @Test
    void testToStringPreservesFullStackTrace() {
        Exception e = createExceptionWithStackTrace();
        String result = ThrowableUtil.toString(e);

        // Should contain multiple stack trace lines
        long atCount = result.chars().filter(ch -> ch == '\n').count();
        assertTrue(atCount > 0);
    }

    private Exception createExceptionWithStackTrace() {
        return methodA();
    }

    private Exception methodA() {
        return methodB();
    }

    private Exception methodB() {
        return methodC();
    }

    private Exception methodC() {
        return new RuntimeException("Deep stack trace");
    }

    // -------------------------------------------------------------------------
    // Test helper classes
    // -------------------------------------------------------------------------

    static class CustomException extends Exception {
        public CustomException(String message) {
            super(message);
        }
    }
}
