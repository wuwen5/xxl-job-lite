/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package com.xxl.job.admin.core.scheduler.cron;

import java.io.Serial;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import lombok.Setter;

/**
 * Provides a parser and evaluator for unix-like cron expressions. Cron
 * expressions provide the ability to specify complex time combinations such as
 * &quot;At 8:00am every Monday through Friday&quot; or &quot;At 1:30am every
 * last Friday of the month&quot;.
 * <p>
 * Cron expressions are comprised of 6 required fields and one optional field
 * separated by white space. The fields respectively are described as follows:
 * </p>
 * <table>
 * <caption>Examples of cron expressions and their meanings.</caption>
 * <tr>
 * <th>Field Name</th>
 * <th>&nbsp;</th>
 * <th>Allowed Values</th>
 * <th>&nbsp;</th>
 * <th>Allowed Special Characters</th>
 * </tr>
 * <tr>
 * <td><code>Seconds</code></td>
 * <td>&nbsp;</td>
 * <td><code>0-59</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td><code>Minutes</code></td>
 * <td>&nbsp;</td>
 * <td><code>0-59</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td><code>Hours</code></td>
 * <td>&nbsp;</td>
 * <td><code>0-23</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td><code>Day-of-month</code></td>
 * <td>&nbsp;</td>
 * <td><code>1-31</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * ? / L W</code></td>
 * </tr>
 * <tr>
 * <td><code>Month</code></td>
 * <td>&nbsp;</td>
 * <td><code>1-12 or JAN-DEC</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td><code>Day-of-Week</code></td>
 * <td>&nbsp;</td>
 * <td><code>1-7 or SUN-SAT</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * ? / L #</code></td>
 * </tr>
 * <tr>
 * <td><code>Year (Optional)</code></td>
 * <td>&nbsp;</td>
 * <td><code>empty, 1970-2199</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * /</code></td>
 * </tr>
 * </table>
 * <p>
 * The '*' character is used to specify all values. For example, &quot;*&quot;
 * in the minute field means &quot;every minute&quot;.
 * </p>
 * <p>
 * The '?' character is allowed for the day-of-month and day-of-week fields. It
 * is used to specify 'no specific value'. This is useful when you need to
 * specify something in one of the two fields, but not the other.
 * <p>
 * The '-' character is used to specify ranges For example &quot;10-12&quot; in
 * the hour field means &quot;the hours 10, 11 and 12&quot;.
 * <p>
 * The ',' character is used to specify additional values. For example
 * &quot;MON,WED,FRI&quot; in the day-of-week field means &quot;the days Monday,
 * Wednesday, and Friday&quot;.
 * </p>
 * <p>
 * The '/' character is used to specify increments. For example &quot;0/15&quot;
 * in the seconds field means &quot;the seconds 0, 15, 30, and 45&quot;. And
 * &quot;5/15&quot; in the seconds field means &quot;the seconds 5, 20, 35, and
 * 50&quot;.  Specifying '*' before the  '/' is equivalent to specifying 0 is
 * the value to start with. Essentially, for each field in the expression, there
 * is a set of numbers that can be turned on or off. For seconds and minutes,
 * the numbers range from 0 to 59. For hours 0 to 23, for days of the month 0 to
 * 31, and for months 0 to 11 (JAN to DEC). The &quot;/&quot; character simply helps you turn
 * on every &quot;nth&quot; value in the given set. Thus &quot;7/6&quot; in the
 * month field only turns on month &quot;7&quot;, it does NOT mean every 6th
 * month, please note that subtlety.
 * </p>
 * <p>
 * The 'L' character is allowed for the day-of-month and day-of-week fields.
 * This character is short-hand for &quot;last&quot;, but it has different
 * meaning in each of the two fields. For example, the value &quot;L&quot; in
 * the day-of-month field means &quot;the last day of the month&quot; - day 31
 * for January, day 28 for February on non-leap years. If used in the
 * day-of-week field by itself, it simply means &quot;7&quot; or
 * &quot;SAT&quot;. But if used in the day-of-week field after another value, it
 * means &quot;the last xxx day of the month&quot; - for example &quot;6L&quot;
 * means &quot;the last friday of the month&quot;. You can also specify an offset
 * from the last day of the month, such as "L-3" which would mean the third-to-last
 * day of the calendar month. <i>When using the 'L' option, it is important not to
 * specify lists, or ranges of values, as you'll get confusing/unexpected results.</i>
 * </p>
 * <p>
 * The 'W' character is allowed for the day-of-month field.  This character
 * is used to specify the weekday (Monday-Friday) nearest the given day.  As an
 * example, if you were to specify &quot;15W&quot; as the value for the
 * day-of-month field, the meaning is: &quot;the nearest weekday to the 15th of
 * the month&quot;. So if the 15th is a Saturday, the trigger will fire on
 * Friday the 14th. If the 15th is a Sunday, the trigger will fire on Monday the
 * 16th. If the 15th is a Tuesday, then it will fire on Tuesday the 15th.
 * However if you specify &quot;1W&quot; as the value for day-of-month, and the
 * 1st is a Saturday, the trigger will fire on Monday the 3rd, as it will not
 * 'jump' over the boundary of a month's days.  The 'W' character can only be
 * specified when the day-of-month is a single day, not a range or list of days.
 * </p>
 * <p>
 * The 'L' and 'W' characters can also be combined for the day-of-month
 * expression to yield 'LW', which translates to &quot;last weekday of the
 * month&quot;.
 * </p>
 * <p>
 * The '#' character is allowed for the day-of-week field. This character is
 * used to specify &quot;the nth&quot; XXX day of the month. For example, the
 * value of &quot;6#3&quot; in the day-of-week field means the third Friday of
 * the month (day 6 = Friday and &quot;#3&quot; = the 3rd one in the month).
 * Other examples: &quot;2#1&quot; = the first Monday of the month and
 * &quot;4#5&quot; = the fifth Wednesday of the month. Note that if you specify
 * &quot;#5&quot; and there is not 5 of the given day-of-week in the month, then
 * no firing will occur that month.  If the '#' character is used, there can
 * only be one expression in the day-of-week field (&quot;3#1,6#3&quot; is
 * not valid, since there are two expressions).
 * </p>
 * <!--The 'C' character is allowed for the day-of-month and day-of-week fields.
 * This character is short-hand for "calendar". This means values are
 * calculated against the associated calendar, if any. If no calendar is
 * associated, then it is equivalent to having an all-inclusive calendar. A
 * value of "5C" in the day-of-month field means "the first day included by the
 * calendar on or after the 5th". A value of "1C" in the day-of-week field
 * means "the first day included by the calendar on or after Sunday".-->
 * <p>
 * The legal characters and the names of months and days of the week are not
 * case sensitive.
 *
 * <p>
 * <b>NOTES:</b>
 * </p>
 * <ul>
 * <li>Support for specifying both a day-of-week and a day-of-month value is
 * not complete (you'll need to use the '?' character in one of these fields).
 * </li>
 * <li>Overflowing ranges is supported - that is, having a larger number on
 * the left hand side than the right. You might do 22-2 to catch 10 o'clock
 * at night until 2 o'clock in the morning, or you might have NOV-FEB. It is
 * very important to note that overuse of overflowing ranges creates ranges
 * that don't make sense and no effort has been made to determine which
 * interpretation CronExpression chooses. An example would be
 * "0 0 14-6 ? * FRI-MON". </li>
 * </ul>
 *
 * Copy from <a href="https://github.com/quartz-scheduler/quartz/blob/main/quartz/src/main/java/org/quartz/CronExpression.java">...</a>
 * v2.5.2 + ade2324
 *
 * @author Sharada Jambula, James House
 * @author Contributions from Mads Henderson
 * @author Refactoring from CronTrigger to CronExpression by Aaron Craven
 */
public final class CronExpression implements Serializable {

    @Serial
    private static final long serialVersionUID = 12423409423L;

    private static final int SECOND = 0;
    private static final int MINUTE = 1;
    private static final int HOUR = 2;
    private static final int DAY_OF_MONTH = 3;
    private static final int MONTH = 4;
    private static final int DAY_OF_WEEK = 5;
    private static final int YEAR = 6;

    /**
     * '*'
     */
    private static final int ALL_SPEC_INT = 99;

    /**
     * '?'
     */
    private static final int NO_SPEC_INT = 98;

    private static final int MAX_LAST_DAY_OFFSET = 30;

    /**
     * "L-30"
     */
    private static final int LAST_DAY_OFFSET_START = 32;

    /**
     * 'L'
     */
    private static final int LAST_DAY_OFFSET_END = LAST_DAY_OFFSET_START + MAX_LAST_DAY_OFFSET;

    private static final Integer ALL_SPEC = ALL_SPEC_INT;
    private static final Integer NO_SPEC = NO_SPEC_INT;

    private static final Map<String, Integer> MONTH_MAP = new HashMap<>(20);
    private static final Map<String, Integer> DAY_MAP = new HashMap<>(60);

    static {
        MONTH_MAP.put("JAN", 0);
        MONTH_MAP.put("FEB", 1);
        MONTH_MAP.put("MAR", 2);
        MONTH_MAP.put("APR", 3);
        MONTH_MAP.put("MAY", 4);
        MONTH_MAP.put("JUN", 5);
        MONTH_MAP.put("JUL", 6);
        MONTH_MAP.put("AUG", 7);
        MONTH_MAP.put("SEP", 8);
        MONTH_MAP.put("OCT", 9);
        MONTH_MAP.put("NOV", 10);
        MONTH_MAP.put("DEC", 11);

        DAY_MAP.put("SUN", 1);
        DAY_MAP.put("MON", 2);
        DAY_MAP.put("TUE", 3);
        DAY_MAP.put("WED", 4);
        DAY_MAP.put("THU", 5);
        DAY_MAP.put("FRI", 6);
        DAY_MAP.put("SAT", 7);
    }

    private final String cronExpression;

    @Setter
    private TimeZone timeZone = null;

    private transient TreeSet<Integer> seconds;
    private transient TreeSet<Integer> minutes;
    private transient TreeSet<Integer> hours;
    private transient TreeSet<Integer> daysOfMonth;
    private transient TreeSet<Integer> nearestWeekdays;
    private transient TreeSet<Integer> months;
    private transient TreeSet<Integer> daysOfWeek;
    private transient TreeSet<Integer> years;

    private transient boolean lastDayOfWeek = false;
    private transient int nthDayOfWeek = 0;

    public static final int MAX_YEAR = Calendar.getInstance().get(Calendar.YEAR) + 100;

    /**
     * Constructs a new <CODE>CronExpression</CODE> based on the specified
     * parameter.
     *
     * @param cronExpression String representation of the cron expression the
     *                       new object should represent
     * @throws java.text.ParseException
     *         if the string expression cannot be parsed into a valid
     *         <CODE>CronExpression</CODE>
     */
    public CronExpression(String cronExpression) throws ParseException {
        if (cronExpression == null) {
            throw new IllegalArgumentException("cronExpression cannot be null");
        }

        this.cronExpression = cronExpression.toUpperCase(Locale.US);

        buildExpression(this.cronExpression);
    }

    /**
     * Returns the next date/time <I>after</I> the given date/time which
     * satisfies the cron expression.
     *
     * @param date the date/time at which to begin the search for the next valid
     *             date/time
     * @return the next valid date/time
     */
    public Date getNextValidTimeAfter(Date date) {
        return getTimeAfter(date);
    }

    /**
     * Returns the time zone for which this <code>CronExpression</code>
     * will be resolved.
     */
    public TimeZone getTimeZone() {
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }

        return timeZone;
    }

    /**
     * Returns the string representation of the <CODE>CronExpression</CODE>
     *
     * @return a string representation of the <CODE>CronExpression</CODE>
     */
    @Override
    public String toString() {
        return cronExpression;
    }

    /**
     * Indicates whether the specified cron expression can be parsed into a
     * valid cron expression
     *
     * @param cronExpression the expression to evaluate
     * @return a boolean indicating whether the given expression is a valid cron
     *         expression
     */
    public static boolean isValidExpression(String cronExpression) {

        try {
            new CronExpression(cronExpression);
        } catch (IllegalArgumentException | ParseException pe) {
            return false;
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Expression Parsing Functions
    //
    ////////////////////////////////////////////////////////////////////////////

    private void buildExpression(String expression) throws ParseException {
        try {

            if (seconds == null) {
                seconds = new TreeSet<>();
            }
            if (minutes == null) {
                minutes = new TreeSet<>();
            }
            if (hours == null) {
                hours = new TreeSet<>();
            }
            if (daysOfMonth == null) {
                daysOfMonth = new TreeSet<>();
            }
            if (nearestWeekdays == null) {
                nearestWeekdays = new TreeSet<>();
            }
            if (months == null) {
                months = new TreeSet<>();
            }
            if (daysOfWeek == null) {
                daysOfWeek = new TreeSet<>();
            }
            if (years == null) {
                years = new TreeSet<>();
            }

            int exprOn = SECOND;

            StringTokenizer exprsTok = new StringTokenizer(expression, " \t", false);

            if (exprsTok.countTokens() > 7) {
                throw new ParseException("Invalid expression has too many terms: " + expression, -1);
            }

            while (exprsTok.hasMoreTokens() && exprOn <= YEAR) {
                String expr = exprsTok.nextToken().trim();

                // throw an exception if L is used with other days of the week
                if (exprOn == DAY_OF_WEEK && expr.indexOf('L') != -1 && expr.length() > 1 && expr.contains(",")) {
                    throw new ParseException(
                            "Support for specifying 'L' with other days of the week is not implemented", -1);
                }
                if (exprOn == DAY_OF_WEEK
                        && expr.indexOf('#') != -1
                        && expr.indexOf('#', expr.indexOf('#') + 1) != -1) {
                    throw new ParseException("Support for specifying multiple \"nth\" days is not implemented.", -1);
                }

                StringTokenizer vTok = new StringTokenizer(expr, ",");
                while (vTok.hasMoreTokens()) {
                    String v = vTok.nextToken();
                    storeExpressionVals(v, exprOn);
                }

                exprOn++;
            }

            if (exprOn <= DAY_OF_WEEK) {
                throw new ParseException("Unexpected end of expression.", expression.length());
            }

            if (exprOn == YEAR) {
                storeExpressionVals("*", YEAR);
            }

            TreeSet<Integer> dow = getSet(DAY_OF_WEEK);
            TreeSet<Integer> dom = getSet(DAY_OF_MONTH);

            // Copying the logic from the UnsupportedOperationException below
            boolean dayOfMSpec = !dom.contains(NO_SPEC);
            boolean dayOfWSpec = !dow.contains(NO_SPEC);

            if (!dayOfMSpec || dayOfWSpec) {
                if (!dayOfWSpec || dayOfMSpec) {
                    throw new ParseException(
                            "Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.",
                            0);
                }
            }
        } catch (ParseException pe) {
            throw pe;
        } catch (Exception e) {
            throw new ParseException("Illegal cron expression format (" + e + ")", 0);
        }
    }

    private int storeExpressionVals(String s, int type) throws ParseException {

        int incr = 0;
        int i = skipWhiteSpace(0, s);
        if (i >= s.length()) {
            return i;
        }
        char c = s.charAt(i);
        if ((c >= 'A') && (c <= 'Z') && (!s.equals("L")) && (!s.equals("LW")) && (!s.matches("^L-[0-9]*[W]?"))) {
            String sub = s.substring(i, i + 3);
            int sval = -1;
            int eval = -1;
            if (type == MONTH) {
                sval = getMonthNumber(sub) + 1;
                if (sval <= 0) {
                    throw new ParseException("Invalid Month value: '" + sub + "'", i);
                }
                if (s.length() > i + 3) {
                    c = s.charAt(i + 3);
                    if (c == '-') {
                        i += 4;
                        sub = s.substring(i, i + 3);
                        eval = getMonthNumber(sub) + 1;
                        if (eval <= 0) {
                            throw new ParseException("Invalid Month value: '" + sub + "'", i);
                        }
                    }
                }
            } else if (type == DAY_OF_WEEK) {
                sval = getDayOfWeekNumber(sub);
                if (sval < 0) {
                    throw new ParseException("Invalid Day-of-Week value: '" + sub + "'", i);
                }
                if (s.length() > i + 3) {
                    c = s.charAt(i + 3);
                    if (c == '-') {
                        i += 4;
                        sub = s.substring(i, i + 3);
                        eval = getDayOfWeekNumber(sub);
                        if (eval < 0) {
                            throw new ParseException("Invalid Day-of-Week value: '" + sub + "'", i);
                        }
                    } else if (c == '#') {
                        try {
                            i += 4;
                            nthDayOfWeek = Integer.parseInt(s.substring(i));
                            if (nthDayOfWeek < 1 || nthDayOfWeek > 5) {
                                throw new Exception();
                            }
                        } catch (Exception e) {
                            throw new ParseException("A numeric value between 1 and 5 must follow the '#' option", i);
                        }
                    } else if (c == 'L') {
                        lastDayOfWeek = true;
                        i++;
                    }
                }

            } else {
                throw new ParseException("Illegal characters for this position: '" + sub + "'", i);
            }
            if (eval != -1) {
                incr = 1;
            }
            addToSet(sval, eval, incr, type);
            return (i + 3);
        }

        if (c == '?') {
            i++;
            if ((i + 1) < s.length() && (s.charAt(i) != ' ' && s.charAt(i + 1) != '\t')) {
                throw new ParseException("Illegal character after '?': " + s.charAt(i), i);
            }
            if (type != DAY_OF_WEEK && type != DAY_OF_MONTH) {
                throw new ParseException("'?' can only be specified for Day-of-Month or Day-of-Week.", i);
            }
            if (type == DAY_OF_WEEK) {
                if (!daysOfMonth.isEmpty() && daysOfMonth.last() == NO_SPEC_INT) {
                    throw new ParseException("'?' can only be specified for Day-of-Month -OR- Day-of-Week.", i);
                }
            }

            addToSet(NO_SPEC_INT, -1, 0, type);
            return i;
        }

        if (c == '*' || c == '/') {
            if (c == '*' && (i + 1) >= s.length()) {
                addToSet(ALL_SPEC_INT, -1, incr, type);
                return i + 1;
            } else if (c == '/' && ((i + 1) >= s.length() || s.charAt(i + 1) == ' ' || s.charAt(i + 1) == '\t')) {
                throw new ParseException("'/' must be followed by an integer.", i);
            } else if (c == '*') {
                i++;
            }
            c = s.charAt(i);
            // is an increment specified?
            if (c == '/') {
                i++;
                if (i >= s.length()) {
                    throw new ParseException("Unexpected end of string.", i);
                }

                incr = getNumericValue(s, i);

                i++;
                if (incr > 10) {
                    i++;
                }
                checkIncrementRange(incr, type, i);
            } else {
                incr = 1;
            }

            addToSet(ALL_SPEC_INT, -1, incr, type);
            return i;
        } else if (c == 'L') {

            if (type < DAY_OF_MONTH) {
                throw new ParseException("'L' not expected in seconds, minutes or hours fields.", i);
            }

            i++;
            if (type == DAY_OF_WEEK) {
                addToSet(7, 7, 0, type);
            }
            if (type == DAY_OF_MONTH) {
                int dom = LAST_DAY_OFFSET_END;
                boolean nearestWeekday = false;
                if (s.length() > i) {
                    c = s.charAt(i);
                    if (c == '-') {
                        ValueSet vs = getValue(0, s, i + 1);
                        int offset = vs.value;
                        if (offset > MAX_LAST_DAY_OFFSET) {
                            throw new ParseException("Offset from last day must be <= " + MAX_LAST_DAY_OFFSET, i + 1);
                        }
                        dom -= offset;
                        i = vs.pos;
                    }
                    if (s.length() > i) {
                        c = s.charAt(i);
                        if (c == 'W') {
                            nearestWeekday = true;
                            i++;
                        }
                    }
                }
                if (nearestWeekday) {
                    nearestWeekdays.add(dom);
                } else {
                    daysOfMonth.add(dom);
                }
            }
            return i;
        } else if (c >= '0' && c <= '9') {
            int val = Integer.parseInt(String.valueOf(c));
            i++;
            if (i >= s.length()) {
                addToSet(val, -1, -1, type);
            } else {
                c = s.charAt(i);
                if (c >= '0' && c <= '9') {
                    ValueSet vs = getValue(val, s, i);
                    val = vs.value;
                    i = vs.pos;
                }
                i = checkNext(i, s, val, type);
                return i;
            }
        } else {
            throw new ParseException("Unexpected character: " + c, i);
        }

        return i;
    }

    private void checkIncrementRange(int incr, int type, int idxPos) throws ParseException {
        if (incr > 59 && (type == SECOND || type == MINUTE)) {
            throw new ParseException("Increment >= 60 : " + incr, idxPos);
        } else if (incr > 23 && (type == HOUR)) {
            throw new ParseException("Increment >= 24 : " + incr, idxPos);
        } else if (incr > 31 && (type == DAY_OF_MONTH)) {
            throw new ParseException("Increment >= 31 : " + incr, idxPos);
        } else if (incr > 7 && (type == DAY_OF_WEEK)) {
            throw new ParseException("Increment >= 7 : " + incr, idxPos);
        } else if (incr > 12 && (type == MONTH)) {
            throw new ParseException("Increment >= 12 : " + incr, idxPos);
        }
    }

    private int checkNext(int pos, String s, int val, int type) throws ParseException {

        int end = -1;
        int i = pos;

        if (i >= s.length()) {
            addToSet(val, end, -1, type);
            return i;
        }

        char c = s.charAt(pos);

        if (c == 'L') {
            if (type == DAY_OF_WEEK) {
                if (val < 1 || val > 7) {
                    throw new ParseException("Day-of-Week values must be between 1 and 7", -1);
                }
                lastDayOfWeek = true;
            } else {
                throw new ParseException("'L' option is not valid here. (pos=" + i + ")", i);
            }
            TreeSet<Integer> set = getSet(type);
            set.add(val);
            i++;
            return i;
        }

        if (c == 'W') {
            if (type != DAY_OF_MONTH) {
                throw new ParseException("'W' option is not valid here. (pos=" + i + ")", i);
            }
            if (val > 31) {
                throw new ParseException(
                        "The 'W' option does not make sense with values larger than 31 (max number of days in a month)",
                        i);
            }
            nearestWeekdays.add(val);
            i++;
            return i;
        }

        if (c == '#') {
            if (type != DAY_OF_WEEK) {
                throw new ParseException("'#' option is not valid here. (pos=" + i + ")", i);
            }
            i++;
            try {
                nthDayOfWeek = Integer.parseInt(s.substring(i));
                if (nthDayOfWeek < 1 || nthDayOfWeek > 5) {
                    throw new Exception();
                }
            } catch (Exception e) {
                throw new ParseException("A numeric value between 1 and 5 must follow the '#' option", i);
            }

            TreeSet<Integer> set = getSet(type);
            set.add(val);
            i++;
            return i;
        }

        if (c == '-') {
            i++;
            c = s.charAt(i);
            int v = Integer.parseInt(String.valueOf(c));
            end = v;
            i++;
            if (i >= s.length()) {
                addToSet(val, end, 1, type);
                return i;
            }
            c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                ValueSet vs = getValue(v, s, i);
                end = vs.value;
                i = vs.pos;
            }
            if (i < s.length() && ((c = s.charAt(i)) == '/')) {
                i++;
                c = s.charAt(i);
                int v2 = Integer.parseInt(String.valueOf(c));
                i++;
                if (i >= s.length()) {
                    addToSet(val, end, v2, type);
                    return i;
                }
                c = s.charAt(i);
                if (c >= '0' && c <= '9') {
                    ValueSet vs = getValue(v2, s, i);
                    int v3 = vs.value;
                    addToSet(val, end, v3, type);
                    i = vs.pos;
                } else {
                    addToSet(val, end, v2, type);
                }
            } else {
                addToSet(val, end, 1, type);
            }
            return i;
        }

        if (c == '/') {
            if ((i + 1) >= s.length() || s.charAt(i + 1) == ' ' || s.charAt(i + 1) == '\t') {
                throw new ParseException("'/' must be followed by an integer.", i);
            }

            i++;
            c = s.charAt(i);
            int v2 = Integer.parseInt(String.valueOf(c));
            i++;
            if (i >= s.length()) {
                checkIncrementRange(v2, type, i);
                addToSet(val, end, v2, type);
                return i;
            }
            c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                ValueSet vs = getValue(v2, s, i);
                int v3 = vs.value;
                checkIncrementRange(v3, type, i);
                addToSet(val, end, v3, type);
                i = vs.pos;
                return i;
            } else {
                throw new ParseException("Unexpected character '" + c + "' after '/'", i);
            }
        }

        addToSet(val, end, 0, type);
        i++;
        return i;
    }

    private int skipWhiteSpace(int i, String s) {
        for (; i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t'); i++) {}

        return i;
    }

    private int findNextWhiteSpace(int i, String s) {
        for (; i < s.length() && (s.charAt(i) != ' ' || s.charAt(i) != '\t'); i++) {}

        return i;
    }

    private void addToSet(int val, int end, int incr, int type) throws ParseException {

        TreeSet<Integer> set = getSet(type);

        if (type == SECOND || type == MINUTE) {
            if ((val < 0 || val > 59 || end > 59) && (val != ALL_SPEC_INT)) {
                throw new ParseException("Minute and Second values must be between 0 and 59", -1);
            }
        } else if (type == HOUR) {
            if ((val < 0 || val > 23 || end > 23) && (val != ALL_SPEC_INT)) {
                throw new ParseException("Hour values must be between 0 and 23", -1);
            }
        } else if (type == DAY_OF_MONTH) {
            if ((val < 1 || val > 31 || end > 31) && (val != ALL_SPEC_INT) && (val != NO_SPEC_INT)) {
                throw new ParseException("Day of month values must be between 1 and 31", -1);
            }
        } else if (type == MONTH) {
            if ((val < 1 || val > 12 || end > 12) && (val != ALL_SPEC_INT)) {
                throw new ParseException("Month values must be between 1 and 12", -1);
            }
        } else if (type == DAY_OF_WEEK) {
            if ((val == 0 || val > 7 || end > 7) && (val != ALL_SPEC_INT) && (val != NO_SPEC_INT)) {
                throw new ParseException("Day-of-Week values must be between 1 and 7", -1);
            }
        }

        if ((incr == 0 || incr == -1) && val != ALL_SPEC_INT) {
            if (val != -1) {
                set.add(val);
            } else {
                set.add(NO_SPEC);
            }

            return;
        }

        int startAt = val;
        int stopAt = end;

        if (val == ALL_SPEC_INT && incr <= 0) {
            incr = 1;
            // put in a marker, but also fill values
            set.add(ALL_SPEC);
        }

        if (type == SECOND || type == MINUTE) {
            if (stopAt == -1) {
                stopAt = 59;
            }
            if (startAt == ALL_SPEC_INT) {
                startAt = 0;
            }
        } else if (type == HOUR) {
            if (stopAt == -1) {
                stopAt = 23;
            }
            if (startAt == ALL_SPEC_INT) {
                startAt = 0;
            }
        } else if (type == DAY_OF_MONTH) {
            if (stopAt == -1) {
                stopAt = 31;
            }
            if (startAt == ALL_SPEC_INT) {
                startAt = 1;
            }
        } else if (type == MONTH) {
            if (stopAt == -1) {
                stopAt = 12;
            }
            if (startAt == ALL_SPEC_INT) {
                startAt = 1;
            }
        } else if (type == DAY_OF_WEEK) {
            if (stopAt == -1) {
                stopAt = 7;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 1;
            }
        } else if (type == YEAR) {
            if (stopAt == -1) {
                stopAt = MAX_YEAR;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 1970;
            }
        }

        // if the end of the range is before the start, then we need to overflow into
        // the next day, month etc. This is done by adding the maximum amount for that
        // type, and using modulus max to determine the value being added.
        int max = -1;
        if (stopAt < startAt) {
            max = switch (type) {
                case SECOND, MINUTE -> 60;
                case HOUR -> 24;
                case MONTH -> 12;
                case DAY_OF_WEEK -> 7;
                case DAY_OF_MONTH -> 31;
                case YEAR -> throw new IllegalArgumentException("Start year must be less than stop year");
                default -> throw new IllegalArgumentException("Unexpected type encountered");};
            stopAt += max;
        }

        for (int i = startAt; i <= stopAt; i += incr) {
            if (max == -1) {
                // ie: there's no max to overflow over
                set.add(i);
            } else {
                // take the modulus to get the real value
                int i2 = i % max;

                // 1-indexed ranges should not include 0, and should include their max
                if (i2 == 0 && (type == MONTH || type == DAY_OF_WEEK || type == DAY_OF_MONTH)) {
                    i2 = max;
                }

                set.add(i2);
            }
        }
    }

    TreeSet<Integer> getSet(int type) throws ParseException {
        return switch (type) {
            case SECOND -> seconds;
            case MINUTE -> minutes;
            case HOUR -> hours;
            case DAY_OF_MONTH -> daysOfMonth;
            case MONTH -> months;
            case DAY_OF_WEEK -> daysOfWeek;
            case YEAR -> years;
            default -> throw new ParseException("Unexpected type encountered.", -1);
        };
    }

    private ValueSet getValue(int v, String s, int i) {
        char c = s.charAt(i);
        StringBuilder s1 = new StringBuilder(String.valueOf(v));
        while (c >= '0' && c <= '9') {
            s1.append(c);
            i++;
            if (i >= s.length()) {
                break;
            }
            c = s.charAt(i);
        }
        ValueSet val = new ValueSet();

        val.pos = (i < s.length()) ? i : i + 1;
        val.value = Integer.parseInt(s1.toString());
        return val;
    }

    private int getNumericValue(String s, int i) {
        int endOfVal = findNextWhiteSpace(i, s);
        String val = s.substring(i, endOfVal);
        return Integer.parseInt(val);
    }

    private int getMonthNumber(String s) {
        return MONTH_MAP.getOrDefault(s, -1);
    }

    private int getDayOfWeekNumber(String s) {
        return DAY_MAP.getOrDefault(s, -1);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // Computation Functions
    //
    ////////////////////////////////////////////////////////////////////////////

    public Date getTimeAfter(Date afterTime) {

        // Computation is based on Gregorian year only.
        Calendar cl = new java.util.GregorianCalendar(getTimeZone());

        // move ahead one second, since we're computing the time *after* the
        // given time
        afterTime = new Date(afterTime.getTime() + 1000);
        // CronTrigger does not deal with milliseconds
        cl.setTime(afterTime);
        cl.set(Calendar.MILLISECOND, 0);

        boolean gotOne = false;
        // loop until we've computed the next time, or we've past the endTime
        while (!gotOne) {

            // if (endTime != null && cl.getTime().after(endTime)) return null;
            // prevent endless loop...
            if (cl.get(Calendar.YEAR) > 2999) {
                return null;
            }

            SortedSet<Integer> st = null;
            int t = 0;

            int sec = cl.get(Calendar.SECOND);
            int min = cl.get(Calendar.MINUTE);

            // get second.................................................
            st = seconds.tailSet(sec);
            if (!st.isEmpty()) {
                sec = st.first();
            } else {
                sec = seconds.first();
                min++;
                cl.set(Calendar.MINUTE, min);
            }
            cl.set(Calendar.SECOND, sec);

            min = cl.get(Calendar.MINUTE);
            int hr = cl.get(Calendar.HOUR_OF_DAY);
            t = -1;

            // get minute.................................................
            st = minutes.tailSet(min);
            if (!st.isEmpty()) {
                t = min;
                min = st.first();
            } else {
                min = minutes.first();
                hr++;
            }
            if (min != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, min);
                setCalendarHour(cl, hr);
                continue;
            }
            cl.set(Calendar.MINUTE, min);

            hr = cl.get(Calendar.HOUR_OF_DAY);
            int day = cl.get(Calendar.DAY_OF_MONTH);
            t = -1;

            // get hour...................................................
            st = hours.tailSet(hr);
            if (!st.isEmpty()) {
                t = hr;
                hr = st.first();
            } else {
                hr = hours.first();
                day++;
            }
            if (hr != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, 0);
                cl.set(Calendar.DAY_OF_MONTH, day);
                setCalendarHour(cl, hr);
                continue;
            }
            cl.set(Calendar.HOUR_OF_DAY, hr);

            day = cl.get(Calendar.DAY_OF_MONTH);
            int mon = cl.get(Calendar.MONTH) + 1;
            // '+ 1' because calendar is 0-based for this field, and we are
            // 1-based
            t = -1;
            int tmon = mon;

            // get day...................................................
            boolean dayOfMSpec = !daysOfMonth.contains(NO_SPEC);
            boolean dayOfWSpec = !daysOfWeek.contains(NO_SPEC);
            // get day by day of month rule
            if (dayOfMSpec && !dayOfWSpec) {
                Optional<Integer> smallestDay = findSmallestDay(day, mon, cl.get(Calendar.YEAR), daysOfMonth);
                Optional<Integer> smallestDayForWeekday =
                        findSmallestDay(day, mon, cl.get(Calendar.YEAR), nearestWeekdays);
                t = day;
                day = -1;
                if (smallestDayForWeekday.isPresent()) {
                    day = smallestDayForWeekday.get();

                    java.util.Calendar tcal = java.util.Calendar.getInstance(getTimeZone());
                    tcal.set(Calendar.SECOND, 0);
                    tcal.set(Calendar.MINUTE, 0);
                    tcal.set(Calendar.HOUR_OF_DAY, 0);
                    tcal.set(Calendar.DAY_OF_MONTH, day);
                    tcal.set(Calendar.MONTH, mon - 1);
                    tcal.set(Calendar.YEAR, cl.get(Calendar.YEAR));

                    int ldom = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                    int dow = tcal.get(Calendar.DAY_OF_WEEK);

                    if (dow == Calendar.SATURDAY && day == 1) {
                        day += 2;
                    } else if (dow == Calendar.SATURDAY) {
                        day -= 1;
                    } else if (dow == Calendar.SUNDAY && day == ldom) {
                        day -= 2;
                    } else if (dow == Calendar.SUNDAY) {
                        day += 1;
                    }

                    tcal.set(Calendar.SECOND, sec);
                    tcal.set(Calendar.MINUTE, min);
                    tcal.set(Calendar.HOUR_OF_DAY, hr);
                    tcal.set(Calendar.DAY_OF_MONTH, day);
                    tcal.set(Calendar.MONTH, mon - 1);
                    Date nTime = tcal.getTime();
                    if (nTime.before(afterTime)) {
                        day = -1;
                    }
                }

                boolean needAdvance = false;
                if (smallestDay.isPresent()) {
                    if (day == -1 || smallestDay.get() < day) {
                        day = smallestDay.get();
                        needAdvance = true;
                    }
                } else if (day == -1) {
                    day = 1;
                    mon++;
                    needAdvance = true;
                }

                if (needAdvance && (day != t || mon != tmon)) {
                    cl.set(Calendar.SECOND, 0);
                    cl.set(Calendar.MINUTE, 0);
                    cl.set(Calendar.HOUR_OF_DAY, 0);
                    cl.set(Calendar.DAY_OF_MONTH, day);
                    cl.set(Calendar.MONTH, mon - 1);
                    // '- 1' because calendar is 0-based for this field, and we
                    // are 1-based
                    continue;
                }

                // get day by day of week rule
            } else if (dayOfWSpec && !dayOfMSpec) {
                // are we looking for the last XXX day of
                if (lastDayOfWeek) {
                    // the month?
                    // desired
                    int dow = daysOfWeek.first();
                    // d-o-w
                    // current d-o-w
                    int cDow = cl.get(Calendar.DAY_OF_WEEK);
                    int daysToAdd = 0;
                    if (cDow < dow) {
                        daysToAdd = dow - cDow;
                    }
                    if (cDow > dow) {
                        daysToAdd = dow + (7 - cDow);
                    }

                    int lDay = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));

                    // did we already miss the
                    if (day + daysToAdd > lDay) {
                        // last one?
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, 1);
                        cl.set(Calendar.MONTH, mon);
                        // no '- 1' here because we are promoting the month
                        continue;
                    }

                    // find date of last occurrence of this day in this month...
                    while ((day + daysToAdd + 7) <= lDay) {
                        daysToAdd += 7;
                    }

                    day += daysToAdd;

                    if (daysToAdd > 0) {
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, day);
                        cl.set(Calendar.MONTH, mon - 1);
                        // '- 1' here because we are not promoting the month
                        continue;
                    }

                } else if (nthDayOfWeek != 0) {
                    // are we looking for the Nth XXX day in the month?
                    // desired
                    int dow = daysOfWeek.first();
                    // d-o-w
                    // current d-o-w
                    int cDow = cl.get(Calendar.DAY_OF_WEEK);
                    int daysToAdd = 0;
                    if (cDow < dow) {
                        daysToAdd = dow - cDow;
                    } else if (cDow > dow) {
                        daysToAdd = dow + (7 - cDow);
                    }

                    boolean dayShifted = daysToAdd > 0;

                    day += daysToAdd;
                    int weekOfMonth = day / 7;
                    if (day % 7 > 0) {
                        weekOfMonth++;
                    }

                    daysToAdd = (nthDayOfWeek - weekOfMonth) * 7;
                    day += daysToAdd;
                    if (daysToAdd < 0 || day > getLastDayOfMonth(mon, cl.get(Calendar.YEAR))) {
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, 1);
                        cl.set(Calendar.MONTH, mon);
                        // no '- 1' here because we are promoting the month
                        continue;
                    } else if (daysToAdd > 0 || dayShifted) {
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, day);
                        cl.set(Calendar.MONTH, mon - 1);
                        // '- 1' here because we are NOT promoting the month
                        continue;
                    }
                } else {
                    // current d-o-w
                    int cDow = cl.get(Calendar.DAY_OF_WEEK);
                    // desired
                    int dow = daysOfWeek.first();
                    // d-o-w
                    st = daysOfWeek.tailSet(cDow);
                    if (!st.isEmpty()) {
                        dow = st.first();
                    }

                    int daysToAdd = 0;
                    if (cDow < dow) {
                        daysToAdd = dow - cDow;
                    }
                    if (cDow > dow) {
                        daysToAdd = dow + (7 - cDow);
                    }

                    int lDay = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));

                    // will we pass the end of
                    if (day + daysToAdd > lDay) {
                        // the month?
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, 1);
                        cl.set(Calendar.MONTH, mon);
                        // no '- 1' here because we are promoting the month
                        continue;
                    } else if (daysToAdd > 0) {
                        // are we switching days?
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, day + daysToAdd);
                        cl.set(Calendar.MONTH, mon - 1);
                        // '- 1' because calendar is 0-based for this field,
                        // and we are 1-based
                        continue;
                    }
                }
            } else { // dayOfWSpec && !dayOfMSpec
                throw new UnsupportedOperationException(
                        "Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.");
            }
            cl.set(Calendar.DAY_OF_MONTH, day);

            mon = cl.get(Calendar.MONTH) + 1;
            // '+ 1' because calendar is 0-based for this field, and we are
            // 1-based
            int year = cl.get(Calendar.YEAR);
            t = -1;

            // test for expressions that never generate a valid fire date,
            // but keep looping...
            if (year > MAX_YEAR) {
                return null;
            }

            // get month...................................................
            st = months.tailSet(mon);
            if (!st.isEmpty()) {
                t = mon;
                mon = st.first();
            } else {
                mon = months.first();
                year++;
            }
            if (mon != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, 0);
                cl.set(Calendar.HOUR_OF_DAY, 0);
                cl.set(Calendar.DAY_OF_MONTH, 1);
                cl.set(Calendar.MONTH, mon - 1);
                // '- 1' because calendar is 0-based for this field, and we are
                // 1-based
                cl.set(Calendar.YEAR, year);
                continue;
            }
            cl.set(Calendar.MONTH, mon - 1);
            // '- 1' because calendar is 0-based for this field, and we are
            // 1-based

            year = cl.get(Calendar.YEAR);
            t = -1;

            // get year...................................................
            st = years.tailSet(year);
            if (!st.isEmpty()) {
                t = year;
                year = st.first();
            } else {
                // ran out of years...
                return null;
            }

            if (year != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, 0);
                cl.set(Calendar.HOUR_OF_DAY, 0);
                cl.set(Calendar.DAY_OF_MONTH, 1);
                cl.set(Calendar.MONTH, 0);
                // '- 1' because calendar is 0-based for this field, and we are
                // 1-based
                cl.set(Calendar.YEAR, year);
                continue;
            }
            cl.set(Calendar.YEAR, year);

            gotOne = true;
        } // while( !done )

        return cl.getTime();
    }

    /**
     * Advance the calendar to the particular hour paying particular attention
     * to daylight saving problems.
     *
     * @param cal the calendar to operate on
     * @param hour the hour to set
     */
    private void setCalendarHour(Calendar cal, int hour) {
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
        if (cal.get(java.util.Calendar.HOUR_OF_DAY) != hour && hour != 24) {
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour + 1);
        }
    }

    private boolean isLeapYear(int year) {
        return ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0));
    }

    private int getLastDayOfMonth(int monthNum, int year) {

        return switch (monthNum) {
            case 1, 3, 5, 7, 8, 10, 12 -> 31;
            case 2 -> (isLeapYear(year)) ? 29 : 28;
            case 4, 6, 9, 11 -> 30;
            default -> throw new IllegalArgumentException("Illegal month number: " + monthNum);
        };
    }

    private Optional<Integer> findSmallestDay(int day, int mon, int year, TreeSet<Integer> set) {
        if (set.isEmpty()) {
            return Optional.empty();
        }

        final int lastDay = getLastDayOfMonth(mon, year);
        // For "L", "L-1", etc.
        final int smallestDay = Optional.ofNullable(set.ceiling(LAST_DAY_OFFSET_END - (lastDay - day)))
                .map(d -> d - LAST_DAY_OFFSET_START + 1)
                .orElse(Integer.MAX_VALUE);

        // For "1", "2", etc.
        SortedSet<Integer> st = set.subSet(day, LAST_DAY_OFFSET_START);
        // make sure we don't over-run a short month, such as february
        if (!st.isEmpty() && st.first() < smallestDay && st.first() <= lastDay) {
            return Optional.of(st.first());
        }

        if (smallestDay == Integer.MAX_VALUE) {
            return Optional.empty();
        } else {
            return Optional.of(smallestDay + lastDay - LAST_DAY_OFFSET_START + 1);
        }
    }

    @Serial
    private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {

        stream.defaultReadObject();
        try {
            buildExpression(cronExpression);
        } catch (Exception ignore) {
        } // never happens
    }

    static class ValueSet {
        public int value;

        public int pos;
    }
}
