package com.xxl.job.admin.core.scheduler.cron;

import static org.junit.jupiter.api.Assertions.*;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

/**
 * CronExpression 单元测试
 * 测试外部直接使用的三个方法：构造方法、getNextValidTimeAfter、isValidExpression
 */
public class CronExpressionTest {

    // ==================== 构造方法测试 ====================

    /**
     * 测试有效的Cron表达式 - 基本格式
     */
    @Test
    public void testConstructor_ValidBasicExpressions() throws ParseException {
        // 每秒执行
        assertNotNull(new CronExpression("* * * * * ? *"));

        // 每分钟执行
        assertNotNull(new CronExpression("0 * * * * ? *"));

        // 每小时执行
        assertNotNull(new CronExpression("0 0 * * * ? *"));

        // 每天零点执行
        assertNotNull(new CronExpression("0 0 0 * * ? *"));

        // 每月1号零点执行
        assertNotNull(new CronExpression("0 0 0 1 * ? *"));

        // 每年1月1号零点执行
        assertNotNull(new CronExpression("0 0 0 1 1 ? *"));
    }

    /**
     * 测试有效的Cron表达式 - 包含范围
     */
    @Test
    public void testConstructor_ValidRangeExpressions() throws ParseException {
        // 工作时间（9-18点）每小时执行
        assertNotNull(new CronExpression("0 0 9-18 * * ? *"));

        // 工作日执行（只指定星期）
        assertNotNull(new CronExpression("0 0 12 ? * MON-FRI *"));

        // 月份范围
        assertNotNull(new CronExpression("0 0 0 1 JAN-MAR ? *"));
    }

    /**
     * 测试有效的Cron表达式 - 包含增量
     */
    @Test
    public void testConstructor_ValidIncrementExpressions() throws ParseException {
        // 每5秒执行
        assertNotNull(new CronExpression("*/5 * * * * ? *"));

        // 从第5秒开始，每10秒执行
        assertNotNull(new CronExpression("5/10 * * * * ? *"));

        // 每15分钟执行
        assertNotNull(new CronExpression("0 */15 * * * ? *"));

        // 每2小时执行
        assertNotNull(new CronExpression("0 0 */2 * * ? *"));
    }

    /**
     * 测试有效的Cron表达式 - 包含列表
     */
    @Test
    public void testConstructor_ValidListExpressions() throws ParseException {
        // 在多个指定秒数执行
        assertNotNull(new CronExpression("0,15,30,45 * * * * ? *"));

        // 在多个指定分钟执行
        assertNotNull(new CronExpression("0 0,15,30,45 * * * ? *"));

        // 在多个指定小时执行
        assertNotNull(new CronExpression("0 0 9,12,15,18 * * ? *"));

        // 在多个指定星期执行
        assertNotNull(new CronExpression("0 0 12 ? * MON,WED,FRI *"));
    }

    /**
     * 测试有效的Cron表达式 - 特殊字符L（Last）
     */
    @Test
    public void testConstructor_ValidSpecialCharacterL() throws ParseException {
        // 每月最后一天执行
        assertNotNull(new CronExpression("0 0 0 L * ? *"));

        // 每月最后一个星期五执行
        assertNotNull(new CronExpression("0 0 0 ? * 6L *"));

        // 每月倒数第3天执行
        assertNotNull(new CronExpression("0 0 0 L-3 * ? *"));
    }

    /**
     * 测试有效的Cron表达式 - 特殊字符W（Weekday）
     */
    @Test
    public void testConstructor_ValidSpecialCharacterW() throws ParseException {
        // 每月15日最近的工作日执行
        assertNotNull(new CronExpression("0 0 0 15W * ? *"));

        // 每月1日最近的工作日执行
        assertNotNull(new CronExpression("0 0 0 1W * ? *"));
    }

    /**
     * 测试有效的Cron表达式 - 特殊字符#（Nth）
     */
    @Test
    public void testConstructor_ValidSpecialCharacterHash() throws ParseException {
        // 每月第三个星期五执行
        assertNotNull(new CronExpression("0 0 0 ? * 6#3 *"));

        // 每月第一个星期一执行
        assertNotNull(new CronExpression("0 0 0 ? * 2#1 *"));

        // 每月最后一个星期三执行
        assertNotNull(new CronExpression("0 0 0 ? * 4#5 *"));
    }

    /**
     * 测试有效的Cron表达式 - 不区分大小写
     */
    @Test
    public void testConstructor_CaseInsensitive() throws ParseException {
        assertNotNull(new CronExpression("0 0 12 ? * mon-fri *"));
        assertNotNull(new CronExpression("0 0 12 ? * Mon-Fri *"));
        assertNotNull(new CronExpression("0 0 12 ? * MON-FRI *"));
        assertNotNull(new CronExpression("0 0 0 1 jan-dec ? *"));
    }

    /**
     * 测试无效的Cron表达式 - null值
     */
    @Test
    public void testConstructor_NullExpression() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CronExpression(null);
        });
    }

    /**
     * 测试无效的Cron表达式 - 空字符串
     */
    @Test
    public void testConstructor_EmptyExpression() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("");
        });
    }

    /**
     * 测试无效的Cron表达式 - 字段数量不足
     */
    @Test
    public void testConstructor_InsufficientFields() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 12 * *"); // 缺少星期和年份字段
        });
    }

    /**
     * 测试无效的Cron表达式 - 字段数量过多
     */
    @Test
    public void testConstructor_TooManyFields() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 12 * * ? * extra"); // 多了一个字段
        });
    }

    /**
     * 测试无效的Cron表达式 - 非法字符
     */
    @Test
    public void testConstructor_InvalidCharacters() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 12 * * ? $");
        });
    }

    /**
     * 测试无效的Cron表达式 - 秒数值超出范围
     */
    @Test
    public void testConstructor_SecondOutOfRange() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("60 * * * * ? *"); // 秒数最大为59
        });
    }

    /**
     * 测试无效的Cron表达式 - 分钟值超出范围
     */
    @Test
    public void testConstructor_MinuteOutOfRange() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 60 * * * ? *"); // 分钟最大为59
        });
    }

    /**
     * 测试无效的Cron表达式 - 小时值超出范围
     */
    @Test
    public void testConstructor_HourOutOfRange() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 24 * * ? *"); // 小时最大为23
        });
    }

    /**
     * 测试无效的Cron表达式 - 日期值超出范围
     */
    @Test
    public void testConstructor_DayOfMonthOutOfRange() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 0 32 * ? *"); // 日期最大为31
        });
    }

    /**
     * 测试无效的Cron表达式 - 月份值超出范围
     */
    @Test
    public void testConstructor_MonthOutOfRange() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 0 1 13 ? *"); // 月份最大为12
        });
    }

    /**
     * 测试无效的Cron表达式 - 星期值超出范围
     */
    @Test
    public void testConstructor_DayOfWeekOutOfRange() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 0 ? * 8 *"); // 星期最大为7
        });
    }

    /**
     * 测试无效的Cron表达式 - 同时指定日期和星期
     */
    @Test
    public void testConstructor_BothDayOfMonthAndDayOfWeek() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 0 15 * MON *"); // 不能同时指定日期和星期
        });
    }

    /**
     * 测试无效的Cron表达式 - 增量值超出范围
     */
    @Test
    public void testConstructor_IncrementOutOfRange() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0/60 * * * * ? *"); // 分钟增量不能超过59
        });
    }

    /**
     * 测试无效的Cron表达式 - 非法的月份名称
     */
    @Test
    public void testConstructor_InvalidMonthName() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 0 1 FOO ? *");
        });
    }

    /**
     * 测试无效的Cron表达式 - 非法的星期名称
     */
    @Test
    public void testConstructor_InvalidDayOfWeekName() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 0 ? * XYZ *");
        });
    }

    /**
     * 测试无效的Cron表达式 - '?' 用在错误的字段
     */
    @Test
    public void testConstructor_QuestionMarkInWrongField() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("? * * * * ? *"); // '?' 只能用在日期或星期字段
        });
    }

    /**
     * 测试无效的Cron表达式 - 'L' 用在错误的字段
     */
    @Test
    public void testConstructor_LInWrongField() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 L * * * ? *"); // 'L' 不能用在分钟字段
        });
    }

    /**
     * 测试无效的Cron表达式 - 'W' 用在错误的字段（分钟字段）
     */
    @Test
    public void testConstructor_WInWrongField() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0W 0 15 * ? *"); // 'W' 不能用在分钟字段
        });
    }

    /**
     * 测试无效的Cron表达式 - '#' 后面没有数字
     */
    @Test
    public void testConstructor_HashWithoutNumber() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 0 ? * 6# *");
        });
    }

    /**
     * 测试无效的Cron表达式 - '#' 后面的数字超出范围
     */
    @Test
    public void testConstructor_HashNumberOutOfRange() {
        assertThrows(ParseException.class, () -> {
            new CronExpression("0 0 0 ? * 6#6 *"); // #后面只能是1-5
        });
    }

    // ==================== isValidExpression 静态方法测试 ====================

    /**
     * 测试isValidExpression - 有效表达式
     */
    @Test
    public void testIsValidExpression_ValidExpressions() {
        assertTrue(CronExpression.isValidExpression("* * * * * ? *"));
        assertTrue(CronExpression.isValidExpression("0 0 0 * * ? *"));
        assertTrue(CronExpression.isValidExpression("0 */5 * * * ? *"));
        assertTrue(CronExpression.isValidExpression("0 0 12 ? * MON-FRI *"));
        assertTrue(CronExpression.isValidExpression("0 0 0 L * ? *"));
        assertTrue(CronExpression.isValidExpression("0 0 0 15W * ? *"));
        assertTrue(CronExpression.isValidExpression("0 0 0 ? * 6#3 *"));
    }

    /**
     * 测试isValidExpression - null表达式
     */
    @Test
    public void testIsValidExpression_NullExpression() {
        // null会抛出IllegalArgumentException，被捕获后返回false
        assertFalse(CronExpression.isValidExpression(null));
    }

    /**
     * 测试isValidExpression - 空字符串
     */
    @Test
    public void testIsValidExpression_EmptyExpression() {
        assertFalse(CronExpression.isValidExpression(""));
    }

    /**
     * 测试isValidExpression - 无效表达式
     */
    @Test
    public void testIsValidExpression_InvalidExpressions() {
        assertFalse(CronExpression.isValidExpression("invalid"));
        assertFalse(CronExpression.isValidExpression("0 0 12 * *")); // 字段不足
        assertFalse(CronExpression.isValidExpression("60 * * * * ? *")); // 秒数超范围
        assertFalse(CronExpression.isValidExpression("0 60 * * * ? *")); // 分钟超范围
        assertFalse(CronExpression.isValidExpression("0 0 24 * * ? *")); // 小时超范围
        assertFalse(CronExpression.isValidExpression("0 0 0 32 * ? *")); // 日期超范围
        assertFalse(CronExpression.isValidExpression("0 0 0 1 13 ? *")); // 月份超范围
        assertFalse(CronExpression.isValidExpression("0 0 0 ? * 8 *")); // 星期超范围
    }

    // ==================== getNextValidTimeAfter 方法测试 ====================

    /**
     * 测试getNextValidTimeAfter - 每秒执行
     */
    @Test
    public void testGetNextValidTimeAfter_EverySecond() throws ParseException {
        CronExpression cron = new CronExpression("* * * * * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 10, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(2026, nextCal.get(Calendar.YEAR));
        assertEquals(Calendar.MAY, nextCal.get(Calendar.MONTH));
        assertEquals(10, nextCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(12, nextCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, nextCal.get(Calendar.MINUTE));
        assertEquals(1, nextCal.get(Calendar.SECOND)); // 应该是下一秒
    }

    /**
     * 测试getNextValidTimeAfter - 每分钟执行
     */
    @Test
    public void testGetNextValidTimeAfter_EveryMinute() throws ParseException {
        CronExpression cron = new CronExpression("0 * * * * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 10, 12, 30, 45);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(0, nextCal.get(Calendar.SECOND));
        assertEquals(31, nextCal.get(Calendar.MINUTE)); // 下一分钟
        assertEquals(12, nextCal.get(Calendar.HOUR_OF_DAY));
    }

    /**
     * 测试getNextValidTimeAfter - 每小时执行
     */
    @Test
    public void testGetNextValidTimeAfter_EveryHour() throws ParseException {
        CronExpression cron = new CronExpression("0 0 * * * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 10, 12, 30, 45);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(0, nextCal.get(Calendar.SECOND));
        assertEquals(0, nextCal.get(Calendar.MINUTE));
        assertEquals(13, nextCal.get(Calendar.HOUR_OF_DAY)); // 下一小时
    }

    /**
     * 测试getNextValidTimeAfter - 每天执行
     */
    @Test
    public void testGetNextValidTimeAfter_EveryDay() throws ParseException {
        CronExpression cron = new CronExpression("0 0 12 * * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 10, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(0, nextCal.get(Calendar.SECOND));
        assertEquals(0, nextCal.get(Calendar.MINUTE));
        assertEquals(12, nextCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(10, nextCal.get(Calendar.DAY_OF_MONTH)); // 同一天
    }

    /**
     * 测试getNextValidTimeAfter - 已过当天时间，应返回第二天
     */
    @Test
    public void testGetNextValidTimeAfter_PastToday_ReturnsTomorrow() throws ParseException {
        CronExpression cron = new CronExpression("0 0 12 * * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 10, 14, 0, 0); // 下午2点，已过12点
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(12, nextCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(11, nextCal.get(Calendar.DAY_OF_MONTH)); // 第二天
    }

    /**
     * 测试getNextValidTimeAfter - 每周工作日执行
     */
    @Test
    public void testGetNextValidTimeAfter_Weekdays() throws ParseException {
        CronExpression cron = new CronExpression("0 0 12 ? * MON-FRI *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 9, 12, 0, 0); // 星期六
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        int dayOfWeek = nextCal.get(Calendar.DAY_OF_WEEK);
        assertTrue(dayOfWeek == Calendar.MONDAY
                || dayOfWeek == Calendar.TUESDAY
                || dayOfWeek == Calendar.WEDNESDAY
                || dayOfWeek == Calendar.THURSDAY
                || dayOfWeek == Calendar.FRIDAY);
    }

    /**
     * 测试getNextValidTimeAfter - 每月1号执行
     */
    @Test
    public void testGetNextValidTimeAfter_FirstOfMonth() throws ParseException {
        CronExpression cron = new CronExpression("0 0 0 1 * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 15, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(1, nextCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.JUNE, nextCal.get(Calendar.MONTH)); // 下个月1号
    }

    /**
     * 测试getNextValidTimeAfter - 每月最后一天执行
     */
    @Test
    public void testGetNextValidTimeAfter_LastDayOfMonth() throws ParseException {
        CronExpression cron = new CronExpression("0 0 0 L * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 15, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        // 5月有31天
        assertEquals(31, nextCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.MAY, nextCal.get(Calendar.MONTH));
    }

    /**
     * 测试getNextValidTimeAfter - 每5分钟执行
     */
    @Test
    public void testGetNextValidTimeAfter_Every5Minutes() throws ParseException {
        CronExpression cron = new CronExpression("0 */5 * * * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 10, 12, 13, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(0, nextCal.get(Calendar.SECOND));
        assertEquals(15, nextCal.get(Calendar.MINUTE)); // 下一个5分钟倍数
    }

    /**
     * 测试getNextValidTimeAfter - 指定多个时间点
     */
    @Test
    public void testGetNextValidTimeAfter_MultipleTimes() throws ParseException {
        CronExpression cron = new CronExpression("0 0 9,12,15,18 * * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 10, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(12, nextCal.get(Calendar.HOUR_OF_DAY)); // 下一个指定时间是12点
    }

    /**
     * 测试getNextValidTimeAfter - 跨月计算
     */
    @Test
    public void testGetNextValidTimeAfter_CrossMonth() throws ParseException {
        CronExpression cron = new CronExpression("0 0 0 1 * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.DECEMBER, 15, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(1, nextCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.JANUARY, nextCal.get(Calendar.MONTH));
        assertEquals(2027, nextCal.get(Calendar.YEAR)); // 跨年
    }

    /**
     * 测试getNextValidTimeAfter - 闰年2月最后一天
     */
    @Test
    public void testGetNextValidTimeAfter_LeapYearFebruary() throws ParseException {
        CronExpression cron = new CronExpression("0 0 0 L * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.FEBRUARY, 15, 12, 0, 0); // 2024是闰年
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(29, nextCal.get(Calendar.DAY_OF_MONTH)); // 闰年2月有29天
        assertEquals(Calendar.FEBRUARY, nextCal.get(Calendar.MONTH));
    }

    /**
     * 测试getNextValidTimeAfter - 非闰年2月最后一天
     */
    @Test
    public void testGetNextValidTimeAfter_NonLeapYearFebruary() throws ParseException {
        CronExpression cron = new CronExpression("0 0 0 L * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.FEBRUARY, 15, 12, 0, 0); // 2025不是闰年
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(28, nextCal.get(Calendar.DAY_OF_MONTH)); // 非闰年2月有28天
        assertEquals(Calendar.FEBRUARY, nextCal.get(Calendar.MONTH));
    }

    /**
     * 测试getNextValidTimeAfter - 第三个星期五
     */
    @Test
    public void testGetNextValidTimeAfter_NthFriday() throws ParseException {
        CronExpression cron = new CronExpression("0 0 0 ? * 6#3 *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(Calendar.FRIDAY, nextCal.get(Calendar.DAY_OF_WEEK));
        // 验证是第三个星期五（日期应该在15-21之间）
        int day = nextCal.get(Calendar.DAY_OF_MONTH);
        assertTrue(day >= 15 && day <= 21);
    }

    /**
     * 测试getNextValidTimeAfter - 时区处理
     */
    @Test
    public void testGetNextValidTimeAfter_TimeZone() throws ParseException {
        CronExpression cron = new CronExpression("0 0 12 * * ? *");
        cron.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        cal.set(2026, Calendar.MAY, 10, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        nextCal.setTime(nextTime);

        assertEquals(12, nextCal.get(Calendar.HOUR_OF_DAY));
    }

    /**
     * 测试getNextValidTimeAfter - 连续调用
     */
    @Test
    public void testGetNextValidTimeAfter_ConsecutiveCalls() throws ParseException {
        CronExpression cron = new CronExpression("0 0 * * * ? *"); // 每小时

        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 10, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date first = cron.getNextValidTimeAfter(cal.getTime());
        assertNotNull(first);

        Date second = cron.getNextValidTimeAfter(first);
        assertNotNull(second);

        // 第二次应该比第一次晚1小时
        long diff = second.getTime() - first.getTime();
        assertEquals(3600000, diff); // 1小时 = 3600000毫秒
    }

    /**
     * 测试getNextValidTimeAfter - 当前时间正好匹配
     */
    @Test
    public void testGetNextValidTimeAfter_ExactMatch() throws ParseException {
        CronExpression cron = new CronExpression("0 0 12 * * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 10, 12, 0, 0); // 正好是12:00:00
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        // 应该返回下一个匹配时间（明天12点）
        assertEquals(12, nextCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(11, nextCal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * 测试getNextValidTimeAfter - 复杂表达式
     */
    @Test
    public void testGetNextValidTimeAfter_ComplexExpression() throws ParseException {
        // 工作日上午9点到下午6点，每30分钟执行
        CronExpression cron = new CronExpression("0 */30 9-18 ? * MON-FRI *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 11, 8, 45, 0); // 周一上午8:45
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY)); // 9点开始
        assertEquals(0, nextCal.get(Calendar.MINUTE));
        assertEquals(Calendar.MONDAY, nextCal.get(Calendar.DAY_OF_WEEK));
    }

    /**
     * 测试getNextValidTimeAfter - 午夜跨越
     */
    @Test
    public void testGetNextValidTimeAfter_MidnightCrossing() throws ParseException {
        CronExpression cron = new CronExpression("0 0 0 * * ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 10, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(0, nextCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, nextCal.get(Calendar.MINUTE));
        assertEquals(0, nextCal.get(Calendar.SECOND));
        assertEquals(11, nextCal.get(Calendar.DAY_OF_MONTH)); // 第二天
    }

    /**
     * 测试getNextValidTimeAfter - 年末跨越
     */
    @Test
    public void testGetNextValidTimeAfter_YearEndCrossing() throws ParseException {
        CronExpression cron = new CronExpression("0 0 0 1 1 ? *");
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.DECEMBER, 31, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date nextTime = cron.getNextValidTimeAfter(cal.getTime());

        assertNotNull(nextTime);
        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextTime);

        assertEquals(2027, nextCal.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, nextCal.get(Calendar.MONTH));
        assertEquals(1, nextCal.get(Calendar.DAY_OF_MONTH));
    }
}
