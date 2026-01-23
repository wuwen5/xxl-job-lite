package com.xxl.job.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * date util
 * public API 保持 Date 不变
 * 内部实现基于 java.time（线程安全、无状态）
 */
public class DateUtil {

    private static final Logger logger = LoggerFactory.getLogger(DateUtil.class);

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_FORMAT);

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATETIME_FORMAT);

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    // ---------------------- format ----------------------

    public static String formatDate(Date date) {
        return format(date, DATE_FORMAT);
    }

    public static String formatDateTime(Date date) {
        return format(date, DATETIME_FORMAT);
    }

    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        try {
            Instant instant = date.toInstant();
            if (DATE_FORMAT.equals(pattern)) {
                return DATE_FORMATTER.format(instant.atZone(DEFAULT_ZONE).toLocalDate());
            }
            if (DATETIME_FORMAT.equals(pattern)) {
                return DATETIME_FORMATTER.format(instant.atZone(DEFAULT_ZONE).toLocalDateTime());
            }

            return DateTimeFormatter.ofPattern(pattern)
                    .format(instant.atZone(DEFAULT_ZONE));
        } catch (Exception e) {
            logger.warn("format date error, date = {}, pattern = {}, errorMsg = {}",
                    date, pattern, e.getMessage());
            return null;
        }
    }

    // ---------------------- parse ----------------------

    public static Date parseDate(String dateString) {
        return parse(dateString, DATE_FORMAT);
    }

    public static Date parseDateTime(String dateString) {
        return parse(dateString, DATETIME_FORMAT);
    }

    public static Date parse(String dateString, String pattern) {
        if (dateString == null) {
            return null;
        }
        try {
            if (DATE_FORMAT.equals(pattern)) {
                LocalDate localDate = LocalDate.parse(dateString, DATE_FORMATTER);
                return Date.from(localDate.atStartOfDay(DEFAULT_ZONE).toInstant());
            }
            if (DATETIME_FORMAT.equals(pattern)) {
                LocalDateTime localDateTime =
                        LocalDateTime.parse(dateString, DATETIME_FORMATTER);
                return Date.from(localDateTime.atZone(DEFAULT_ZONE).toInstant());
            }

            LocalDateTime localDateTime =
                    LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(pattern));
            return Date.from(localDateTime.atZone(DEFAULT_ZONE).toInstant());
        } catch (Exception e) {
            logger.warn(
                    "parse date error, dateString = {}, pattern = {}, errorMsg = {}",
                    dateString, pattern, e.getMessage()
            );
            return null;
        }
    }

    // ---------------------- add date ----------------------

    public static Date addYears(final Date date, final int amount) {
        return add(date, amount, TimeUnit.YEAR);
    }

    public static Date addMonths(final Date date, final int amount) {
        return add(date, amount, TimeUnit.MONTH);
    }

    public static Date addDays(final Date date, final int amount) {
        return add(date, amount, TimeUnit.DAY);
    }

    public static Date addHours(final Date date, final int amount) {
        return add(date, amount, TimeUnit.HOUR);
    }

    public static Date addMinutes(final Date date, final int amount) {
        return add(date, amount, TimeUnit.MINUTE);
    }

    private static Date add(Date date, int amount, TimeUnit unit) {
        if (date == null) {
            return null;
        }
        try {
            LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), DEFAULT_ZONE);
            switch (unit) {
                case YEAR:
                    ldt = ldt.plusYears(amount);
                    break;
                case MONTH:
                    ldt = ldt.plusMonths(amount);
                    break;
                case DAY:
                    ldt = ldt.plusDays(amount);
                    break;
                case HOUR:
                    ldt = ldt.plusHours(amount);
                    break;
                case MINUTE:
                    ldt = ldt.plusMinutes(amount);
                    break;
                default:
            }
            return Date.from(ldt.atZone(DEFAULT_ZONE).toInstant());
        } catch (Exception e) {
            logger.warn("add date error, date = {}, amount = {}, unit = {}, errorMsg = {}",
                    date, amount, unit, e.getMessage());
            return null;
        }
    }

    private enum TimeUnit {
        /**
         * 年
         */
        YEAR,
        /**
         * 月
         */
        MONTH,
        /**
         * 日
         */
        DAY,
        /**
         * 小时
         */
        HOUR,
        /**
         * 分钟
         */
        MINUTE
    }
}
