package com.xxl.job.admin.core.thread;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xxl.job.admin.AbstractTest;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.scheduler.MisfireStrategyEnum;
import com.xxl.job.admin.core.scheduler.ScheduleTypeEnum;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import jakarta.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * JobScheduleHelper unit test
 */
class JobScheduleHelperTest extends AbstractTest {

    @MockitoBean
    private XxlJobInfoDao jobInfoDao;

    @Resource
    private JdbcTemplate jdbcTemplate;

    private JobTriggerPoolHelper originalTriggerPoolHelper;
    private JobTriggerPoolHelper triggerPoolHelperSpy;

    @BeforeAll
    static void setUpClass() throws Exception {
        // Reset singleton first so toStop operates on a fresh instance without threads
        resetSingletonInstance();
    }

    @BeforeEach
    void setUp() throws Exception {
        reset(jobInfoDao);
        resetSingletonInstance();
        ensureScheduleLockExists();

        // Replace JobTriggerPoolHelper's static helper instance with a Mockito spy.
        // Static mocking via mockStatic() is thread-local and cannot intercept calls
        // from the schedule thread or verify from Awaitility's poll thread, so we use
        // an instance spy on the internal helper field instead.
        Field helperField = JobTriggerPoolHelper.class.getDeclaredField("helper");
        helperField.setAccessible(true);
        originalTriggerPoolHelper = (JobTriggerPoolHelper) helperField.get(null);
        triggerPoolHelperSpy = spy(originalTriggerPoolHelper);
        helperField.set(null, triggerPoolHelperSpy);
        // Prevent the spy from actually executing the trigger pool submission.
        doNothing().when(triggerPoolHelperSpy).addTrigger(anyInt(), any(), anyInt(), any(), any(), any());
    }

    @AfterEach
    void tearDown() throws Exception {
        clearRingData();
        stopHelper(JobScheduleHelper.getInstance());
        if (triggerPoolHelperSpy != null && originalTriggerPoolHelper != null) {
            Field helperField = JobTriggerPoolHelper.class.getDeclaredField("helper");
            helperField.setAccessible(true);
            helperField.set(null, originalTriggerPoolHelper);
        }
    }

    private void ensureScheduleLockExists() {
        jdbcTemplate.update("MERGE INTO xxl_job_lock (lock_name) KEY (lock_name) VALUES ('schedule_lock')");
    }

    private static void resetSingletonInstance() throws Exception {
        Field instanceField = JobScheduleHelper.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, new JobScheduleHelper());
    }

    private void clearRingData() throws Exception {
        Field ringDataField = JobScheduleHelper.class.getDeclaredField("RING_DATA");
        ringDataField.setAccessible(true);
        Map<?, ?> ringData = (Map<?, ?>) ringDataField.get(null);
        ringData.clear();
    }

    private void stopHelper(JobScheduleHelper helper) throws Exception {
        Field scheduleThreadField = JobScheduleHelper.class.getDeclaredField("scheduleThread");
        scheduleThreadField.setAccessible(true);
        if (scheduleThreadField.get(helper) != null) {
            helper.toStop();
        }
    }

    private void startAndSkipInitialSleep(JobScheduleHelper helper) throws Exception {
        helper.start();
        // The schedule thread sleeps for 5000 - System.currentTimeMillis() % 1000 ms before first scan.
        // Wait for that initial sleep to finish so that the schedule logic is captured.
        long sleepMs = 5000 - System.currentTimeMillis() % 1000 + 200;
        Thread.sleep(sleepMs);
    }

    private void stopScheduleThread(JobScheduleHelper helper) throws Exception {
        Field scheduleThreadToStopField = JobScheduleHelper.class.getDeclaredField("scheduleThreadToStop");
        scheduleThreadToStopField.setAccessible(true);
        scheduleThreadToStopField.setBoolean(helper, true);

        Field scheduleThreadField = JobScheduleHelper.class.getDeclaredField("scheduleThread");
        scheduleThreadField.setAccessible(true);
        Thread scheduleThread = (Thread) scheduleThreadField.get(helper);
        if (scheduleThread != null && scheduleThread.isAlive()) {
            scheduleThread.interrupt();
            scheduleThread.join(1000);
        }
    }

    private void invokeRefreshNextValidTime(JobScheduleHelper helper, XxlJobInfo jobInfo, Date fromTime)
            throws Exception {
        Method method = JobScheduleHelper.class.getDeclaredMethod("refreshNextValidTime", XxlJobInfo.class, Date.class);
        method.setAccessible(true);
        method.invoke(helper, jobInfo, fromTime);
    }

    private void invokePushTimeRing(JobScheduleHelper helper, int ringSecond, int jobId) throws Exception {
        Method method = JobScheduleHelper.class.getDeclaredMethod("pushTimeRing", int.class, int.class);
        method.setAccessible(true);
        method.invoke(helper, ringSecond, jobId);
    }

    private XxlJobInfo createJob(
            int id,
            long triggerNextTime,
            String scheduleType,
            String scheduleConf,
            MisfireStrategyEnum misfireStrategy) {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(id);
        jobInfo.setTriggerStatus(1);
        jobInfo.setTriggerNextTime(triggerNextTime);
        jobInfo.setScheduleType(scheduleType);
        jobInfo.setScheduleConf(scheduleConf);
        jobInfo.setMisfireStrategy(misfireStrategy.name());
        return jobInfo;
    }

    @Test
    void shouldGenerateNextValidTimeForCron() throws Exception {
        XxlJobInfo jobInfo =
                createJob(1, 0, ScheduleTypeEnum.CRON.name(), "0 0 * * * ?", MisfireStrategyEnum.DO_NOTHING);
        Date fromTime = new Date();

        Date nextTime = JobScheduleHelper.generateNextValidTime(jobInfo, fromTime);

        assertNotNull(nextTime);
        assertTrue(nextTime.after(fromTime));
    }

    @Test
    void shouldGenerateNextValidTimeForFixRate() throws Exception {
        XxlJobInfo jobInfo = createJob(1, 0, ScheduleTypeEnum.FIX_RATE.name(), "60", MisfireStrategyEnum.DO_NOTHING);
        Date fromTime = new Date(1609459200000L);

        Date nextTime = JobScheduleHelper.generateNextValidTime(jobInfo, fromTime);

        assertEquals(1609459260000L, nextTime.getTime());
    }

    @Test
    void shouldReturnNullForUnknownScheduleType() throws Exception {
        XxlJobInfo jobInfo = createJob(1, 0, "UNKNOWN", "0 0 * * * ?", MisfireStrategyEnum.DO_NOTHING);

        Date nextTime = JobScheduleHelper.generateNextValidTime(jobInfo, new Date());

        assertNull(nextTime);
    }

    @Test
    void shouldReturnNullWhenScheduleTypeIsNull() throws Exception {
        XxlJobInfo jobInfo = createJob(1, 0, null, "0 0 * * * ?", MisfireStrategyEnum.DO_NOTHING);

        Date nextTime = JobScheduleHelper.generateNextValidTime(jobInfo, new Date());

        assertNull(nextTime);
    }

    @Test
    void shouldRefreshNextValidTimeForCron() throws Exception {
        XxlJobInfo jobInfo = createJob(
                1, 1609459200000L, ScheduleTypeEnum.CRON.name(), "0 0 * * * ?", MisfireStrategyEnum.DO_NOTHING);

        invokeRefreshNextValidTime(JobScheduleHelper.getInstance(), jobInfo, new Date(1609459200000L));

        assertEquals(-1, jobInfo.getTriggerStatus());
        assertEquals(1609459200000L, jobInfo.getTriggerLastTime());
        assertTrue(jobInfo.getTriggerNextTime() > 1609459200000L);
    }

    @Test
    void shouldStopJobWhenCronExpressionInvalid() throws Exception {
        XxlJobInfo jobInfo =
                createJob(1, 1609459200000L, ScheduleTypeEnum.CRON.name(), "invalid", MisfireStrategyEnum.DO_NOTHING);

        invokeRefreshNextValidTime(JobScheduleHelper.getInstance(), jobInfo, new Date());

        assertEquals(0, jobInfo.getTriggerStatus());
        assertEquals(0, jobInfo.getTriggerLastTime());
        assertEquals(0, jobInfo.getTriggerNextTime());
    }

    @Test
    void shouldStopJobWhenFixRateConfigInvalid() throws Exception {
        XxlJobInfo jobInfo =
                createJob(1, 1609459200000L, ScheduleTypeEnum.FIX_RATE.name(), "abc", MisfireStrategyEnum.DO_NOTHING);

        invokeRefreshNextValidTime(JobScheduleHelper.getInstance(), jobInfo, new Date(1609459200000L));

        assertEquals(0, jobInfo.getTriggerStatus());
        assertEquals(0, jobInfo.getTriggerLastTime());
        assertEquals(0, jobInfo.getTriggerNextTime());
    }

    @Test
    void shouldPushJobToTimeRing() throws Exception {
        JobScheduleHelper helper = JobScheduleHelper.getInstance();
        invokePushTimeRing(helper, 30, 100);
        invokePushTimeRing(helper, 30, 101);
        invokePushTimeRing(helper, 45, 200);

        Field ringDataField = JobScheduleHelper.class.getDeclaredField("RING_DATA");
        ringDataField.setAccessible(true);
        Map<Integer, List<Integer>> ringData = (Map<Integer, List<Integer>>) ringDataField.get(null);

        assertEquals(Arrays.asList(100, 101), ringData.get(30));
        assertEquals(Collections.singletonList(200), ringData.get(45));
    }

    @Test
    void shouldStartAndStopThreads() throws Exception {
        JobScheduleHelper helper = JobScheduleHelper.getInstance();
        helper.start();

        Field scheduleThreadField = JobScheduleHelper.class.getDeclaredField("scheduleThread");
        scheduleThreadField.setAccessible(true);
        Thread scheduleThread = (Thread) scheduleThreadField.get(helper);
        assertNotNull(scheduleThread);
        assertTrue(scheduleThread.isAlive());

        Field ringThreadField = JobScheduleHelper.class.getDeclaredField("ringThread");
        ringThreadField.setAccessible(true);
        Thread ringThread = (Thread) ringThreadField.get(helper);
        assertNotNull(ringThread);
        assertTrue(ringThread.isAlive());
    }

    @Test
    void shouldTriggerMisfireJobWhenFireOnceNow() throws Exception {
        long now = System.currentTimeMillis();
        XxlJobInfo jobInfo = createJob(
                1, now - 10000, ScheduleTypeEnum.CRON.name(), "0 0 * * * ?", MisfireStrategyEnum.FIRE_ONCE_NOW);
        when(jobInfoDao.scheduleJobQuery(anyLong(), anyInt()))
                .thenReturn(Collections.singletonList(jobInfo))
                .thenReturn(Collections.emptyList());

        JobScheduleHelper helper = JobScheduleHelper.getInstance();
        startAndSkipInitialSleep(helper);

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> verify(triggerPoolHelperSpy, atLeastOnce())
                .addTrigger(eq(1), eq(TriggerTypeEnum.MISFIRE), eq(-1), any(), any(), any()));

        verify(jobInfoDao, atLeastOnce()).scheduleUpdate(any(XxlJobInfo.class));
        ArgumentCaptor<XxlJobInfo> captor = ArgumentCaptor.forClass(XxlJobInfo.class);
        verify(jobInfoDao, atLeastOnce()).scheduleUpdate(captor.capture());
        XxlJobInfo updated = captor.getValue();
        assertEquals(-1, updated.getTriggerStatus());
        assertTrue(updated.getTriggerNextTime() > now);

        stopScheduleThread(helper);
    }

    @Test
    void shouldDirectTriggerExpiredJobWithinPreReadWindow() throws Exception {
        long now = System.currentTimeMillis();
        // After the initial ~5s sleep the job is expired but still within the pre-read window.
        XxlJobInfo jobInfo =
                createJob(2, now + 3000, ScheduleTypeEnum.CRON.name(), "0 0 * * * ?", MisfireStrategyEnum.DO_NOTHING);
        when(jobInfoDao.scheduleJobQuery(anyLong(), anyInt()))
                .thenReturn(Collections.singletonList(jobInfo))
                .thenReturn(Collections.emptyList());

        JobScheduleHelper helper = JobScheduleHelper.getInstance();
        startAndSkipInitialSleep(helper);

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> verify(triggerPoolHelperSpy, atLeastOnce())
                .addTrigger(eq(2), eq(TriggerTypeEnum.CRON), eq(-1), any(), any(), any()));
        verify(jobInfoDao, atLeastOnce()).scheduleUpdate(any(XxlJobInfo.class));

        stopScheduleThread(helper);
    }

    @Test
    void shouldPushFutureJobToTimeRing() throws Exception {
        long now = System.currentTimeMillis();
        // After the initial ~5s sleep the job is still in the future, so it is pushed to the time ring.
        XxlJobInfo jobInfo =
                createJob(3, now + 8000, ScheduleTypeEnum.CRON.name(), "0 0 * * * ?", MisfireStrategyEnum.DO_NOTHING);
        when(jobInfoDao.scheduleJobQuery(anyLong(), anyInt()))
                .thenReturn(Collections.singletonList(jobInfo))
                .thenReturn(Collections.emptyList());

        JobScheduleHelper helper = JobScheduleHelper.getInstance();
        startAndSkipInitialSleep(helper);

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(jobInfoDao, atLeastOnce()).scheduleUpdate(any(XxlJobInfo.class));
        });
        verify(triggerPoolHelperSpy, never()).addTrigger(anyInt(), any(), anyInt(), any(), any(), any());

        Field ringDataField = JobScheduleHelper.class.getDeclaredField("RING_DATA");
        ringDataField.setAccessible(true);
        Map<Integer, List<Integer>> ringData = (Map<Integer, List<Integer>>) ringDataField.get(null);
        assertFalse(ringData.isEmpty());
        assertTrue(ringData.values().stream().anyMatch(list -> list.contains(3)));

        stopScheduleThread(helper);
    }

    @Test
    void shouldHandleExceptionWhenScheduleThreadError() throws Exception {
        when(jobInfoDao.scheduleJobQuery(anyLong(), anyInt())).thenThrow(new RuntimeException("database error"));

        JobScheduleHelper helper = JobScheduleHelper.getInstance();
        startAndSkipInitialSleep(helper);

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(jobInfoDao, atLeastOnce()).scheduleJobQuery(anyLong(), anyInt());
        });
        verify(jobInfoDao, never()).scheduleUpdate(any());

        stopScheduleThread(helper);
    }
}
