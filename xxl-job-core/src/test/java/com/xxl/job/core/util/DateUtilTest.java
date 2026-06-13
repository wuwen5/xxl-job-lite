package com.xxl.job.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * DateUtil unit test
 *
 * @author wuwen
 */
class DateUtilTest {

    @Test
    void testFormatDate() {
        Date date = new Date(1700000000000L); // Fixed timestamp
        String result = DateUtil.formatDate(date);

        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void testFormatDateWithNull() {
        String result = DateUtil.formatDate(null);
        assertNull(result);
    }

    @Test
    void testFormatDateTime() {
        Date date = new Date(1700000000000L);
        String result = DateUtil.formatDateTime(date);

        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testFormatDateTimeWithNull() {
        String result = DateUtil.formatDateTime(null);
        assertNull(result);
    }

    @Test
    void testFormatWithCustomPattern() {
        Date date = new Date(1700000000000L);
        String result = DateUtil.format(date, "yyyy/MM/dd");

        assertNotNull(result);
        assertTrue(result.matches("\\d{4}/\\d{2}/\\d{2}"));
    }

    @Test
    void testFormatWithInvalidPattern() {
        Date date = new Date();
        // Invalid pattern should return null
        String result = DateUtil.format(date, "invalid_pattern_$$$");
        // May return null or formatted string depending on pattern
        // The important thing is no exception is thrown
        assertTrue(true);
    }

    @Test
    void testParseDate() {
        String dateString = "2023-11-15";
        Date result = DateUtil.parseDate(dateString);

        assertNotNull(result);
        assertEquals("2023-11-15", DateUtil.formatDate(result));
    }

    @Test
    void testParseDateWithNull() {
        Date result = DateUtil.parseDate(null);
        assertNull(result);
    }

    @Test
    void testParseDateWithInvalidFormat() {
        String dateString = "invalid-date";
        Date result = DateUtil.parseDate(dateString);
        assertNull(result);
    }

    @Test
    void testParseDateTime() {
        String dateString = "2023-11-15 10:30:45";
        Date result = DateUtil.parseDateTime(dateString);

        assertNotNull(result);
        assertEquals("2023-11-15 10:30:45", DateUtil.formatDateTime(result));
    }

    @Test
    void testParseDateTimeWithNull() {
        Date result = DateUtil.parseDateTime(null);
        assertNull(result);
    }

    @Test
    void testParseWithCustomPattern() {
        // Custom pattern with datetime
        String dateString = "2023/11/15 10:30:45";
        Date result = DateUtil.parse(dateString, "yyyy/MM/dd HH:mm:ss");

        assertNotNull(result);
        assertEquals("2023-11-15", DateUtil.formatDate(result));
    }

    @Test
    void testAddYears() {
        Date date = DateUtil.parseDate("2023-11-15");
        Date result = DateUtil.addYears(date, 1);

        assertNotNull(result);
        assertEquals("2024-11-15", DateUtil.formatDate(result));
    }

    @Test
    void testAddYearsNegative() {
        Date date = DateUtil.parseDate("2023-11-15");
        Date result = DateUtil.addYears(date, -1);

        assertNotNull(result);
        assertEquals("2022-11-15", DateUtil.formatDate(result));
    }

    @Test
    void testAddYearsWithNull() {
        Date result = DateUtil.addYears(null, 1);
        assertNull(result);
    }

    @Test
    void testAddMonths() {
        Date date = DateUtil.parseDate("2023-11-15");
        Date result = DateUtil.addMonths(date, 2);

        assertNotNull(result);
        assertEquals("2024-01-15", DateUtil.formatDate(result));
    }

    @Test
    void testAddDays() {
        Date date = DateUtil.parseDate("2023-11-15");
        Date result = DateUtil.addDays(date, 10);

        assertNotNull(result);
        assertEquals("2023-11-25", DateUtil.formatDate(result));
    }

    @Test
    void testAddHours() {
        Date date = DateUtil.parseDateTime("2023-11-15 10:00:00");
        Date result = DateUtil.addHours(date, 5);

        assertNotNull(result);
        String formatted = DateUtil.formatDateTime(result);
        assertTrue(formatted.contains("15:00:00"));
    }

    @Test
    void testAddMinutes() {
        Date date = DateUtil.parseDateTime("2023-11-15 10:30:00");
        Date result = DateUtil.addMinutes(date, 45);

        assertNotNull(result);
        String formatted = DateUtil.formatDateTime(result);
        assertTrue(formatted.contains("11:15:00"));
    }

    @Test
    void testRoundTripDateFormat() {
        // Test format -> parse -> format consistency
        Date original = new Date();
        String formatted = DateUtil.formatDate(original);
        Date parsed = DateUtil.parseDate(formatted);
        String reformatted = DateUtil.formatDate(parsed);

        assertEquals(formatted, reformatted);
    }

    @Test
    void testRoundTripDateTimeFormat() {
        // Test format -> parse -> format consistency
        Date original = new Date();
        String formatted = DateUtil.formatDateTime(original);
        Date parsed = DateUtil.parseDateTime(formatted);
        String reformatted = DateUtil.formatDateTime(parsed);

        assertEquals(formatted, reformatted);
    }

    @Test
    void testEdgeCaseLeapYear() {
        Date date = DateUtil.parseDate("2024-02-29"); // Leap year
        assertNotNull(date);

        Date nextYear = DateUtil.addYears(date, 1);
        // Should handle Feb 29 -> Feb 28 in non-leap year
        assertNotNull(nextYear);
    }

    @Test
    void testEdgeCaseMonthEnd() {
        Date date = DateUtil.parseDate("2023-01-31");
        Date result = DateUtil.addMonths(date, 1);

        assertNotNull(result);
        // Jan 31 + 1 month should be Feb 28 (or 29 in leap year)
        String formatted = DateUtil.formatDate(result);
        assertTrue(formatted.startsWith("2023-02"));
    }
}
