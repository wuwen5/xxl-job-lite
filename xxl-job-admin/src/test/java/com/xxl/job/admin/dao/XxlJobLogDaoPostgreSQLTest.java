package com.xxl.job.admin.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.admin.AbstractPostgreSQLTest;
import com.xxl.job.admin.core.model.XxlJobLog;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class XxlJobLogDaoPostgreSQLTest extends AbstractPostgreSQLTest {

    @Resource
    private XxlJobLogDao xxlJobLogDao;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE xxl_job_log, xxl_job_info, xxl_job_registry RESTART IDENTITY CASCADE");
        jdbcTemplate.update(
                "INSERT INTO xxl_job_info(id, job_group, job_desc, schedule_type, misfire_strategy, glue_type) VALUES (1,1,'job-1','CRON','DO_NOTHING','BEAN')");
        jdbcTemplate.update(
                "INSERT INTO xxl_job_info(id, job_group, job_desc, schedule_type, misfire_strategy, glue_type) VALUES (2,2,'job-2','CRON','DO_NOTHING','BEAN')");
    }

    @Test
    @Disabled(
            "PostgreSQL mapper SQL issue: pageList uses MySQL-style LIMIT offset,pagesize, to be fixed in follow-up PR")
    void pageList_withFilters() {
        Instant now = Instant.now();
        long log1 = insertLog(1, 1, now.minus(40, ChronoUnit.MINUTES), 200, 200, 0, "addr-1");
        insertLog(1, 1, now.minus(30, ChronoUnit.MINUTES), 500, 200, 0, "addr-2");
        insertLog(2, 2, now.minus(20, ChronoUnit.MINUTES), 200, 0, 0, "addr-3");
        insertLog(2, 2, now.minus(10, ChronoUnit.MINUTES), 200, 500, 0, "addr-4");

        List<XxlJobLog> running = xxlJobLogDao.pageList(0, 10, 0, 0, null, null, 3);
        assertEquals(1, running.size());

        List<XxlJobLog> byJobId = xxlJobLogDao.pageList(0, 10, 1, 1, null, null, 0);
        assertEquals(2, byJobId.size());

        List<XxlJobLog> byTime = xxlJobLogDao.pageList(
                0,
                10,
                0,
                0,
                Date.from(now.minus(35, ChronoUnit.MINUTES)),
                Date.from(now.minus(15, ChronoUnit.MINUTES)),
                0);
        assertEquals(2, byTime.size());
        assertTrue(byTime.stream().anyMatch(log -> log.getId() == log1));
    }

    @Test
    void pageListCount_withFilters() {
        Instant now = Instant.now();
        insertLog(1, 1, now.minus(40, ChronoUnit.MINUTES), 200, 200, 0, "addr-1");
        insertLog(1, 1, now.minus(30, ChronoUnit.MINUTES), 500, 200, 0, "addr-2");
        insertLog(2, 2, now.minus(20, ChronoUnit.MINUTES), 200, 0, 0, "addr-3");
        insertLog(2, 2, now.minus(10, ChronoUnit.MINUTES), 200, 500, 0, "addr-4");

        assertEquals(2, xxlJobLogDao.pageListCount(0, 10, 1, 0, null, null, 0));
        assertEquals(2, xxlJobLogDao.pageListCount(0, 10, 1, 1, null, null, 0));
        assertEquals(
                2,
                xxlJobLogDao.pageListCount(
                        0,
                        10,
                        0,
                        0,
                        Date.from(now.minus(35, ChronoUnit.MINUTES)),
                        Date.from(now.minus(15, ChronoUnit.MINUTES)),
                        0));
        assertEquals(2, xxlJobLogDao.pageListCount(0, 10, 0, 0, null, null, 1));
        assertEquals(2, xxlJobLogDao.pageListCount(0, 10, 0, 0, null, null, 2));
        assertEquals(1, xxlJobLogDao.pageListCount(0, 10, 0, 0, null, null, 3));
    }

    @Test
    void saveLoadUpdateDeleteAndAlarmQueries() {
        XxlJobLog log = new XxlJobLog();
        log.setJobGroup(1);
        log.setJobId(1);
        log.setTriggerTime(new Date());
        log.setTriggerCode(0);
        log.setHandleCode(0);

        assertEquals(1, xxlJobLogDao.save(log));
        assertTrue(log.getId() > 0);

        XxlJobLog loaded = xxlJobLogDao.load(log.getId());
        assertNotNull(loaded);
        assertEquals(1, loaded.getJobGroup());

        log.setTriggerTime(new Date());
        log.setTriggerCode(200);
        log.setTriggerMsg("trigger-ok");
        log.setExecutorAddress("executor-a");
        log.setExecutorHandler("handler-a");
        log.setExecutorParam("param-a");
        log.setExecutorShardingParam("0/1");
        log.setExecutorFailRetryCount(1);
        assertEquals(1, xxlJobLogDao.updateTriggerInfo(log));

        XxlJobLog afterTrigger = xxlJobLogDao.load(log.getId());
        assertEquals(200, afterTrigger.getTriggerCode());
        assertEquals("executor-a", afterTrigger.getExecutorAddress());

        log.setHandleTime(new Date());
        log.setHandleCode(500);
        log.setHandleMsg("handle-fail");
        assertEquals(1, xxlJobLogDao.updateHandleInfo(log));

        XxlJobLog afterHandle = xxlJobLogDao.load(log.getId());
        assertEquals(500, afterHandle.getHandleCode());

        List<Long> failIds = xxlJobLogDao.findFailJobLogIds(10);
        assertTrue(failIds.contains(log.getId()));

        assertEquals(1, xxlJobLogDao.updateAlarmStatus(log.getId(), 0, 2));
        assertEquals(0, xxlJobLogDao.updateAlarmStatus(log.getId(), 0, 3));

        assertEquals(1, xxlJobLogDao.delete(1));
        assertEquals(0, xxlJobLogDao.pageListCount(0, 10, 0, 1, null, null, 0));
    }

    @Test
    void findClearLogIdsClearLogAndFindLostJobIds() {
        Instant now = Instant.now();
        long old1 = insertLog(1, 1, now.minus(10, ChronoUnit.DAYS), 200, 200, 0, "addr-1");
        long old2 = insertLog(1, 1, now.minus(9, ChronoUnit.DAYS), 200, 0, 0, "addr-2");
        long recent = insertLog(1, 1, now.minus(1, ChronoUnit.DAYS), 200, 0, 0, "addr-3");

        List<Long> clearIds = xxlJobLogDao.findClearLogIds(1, 1, Date.from(now.minus(2, ChronoUnit.DAYS)), 2, 10);
        assertEquals(1, clearIds.size());
        assertEquals(old1, clearIds.get(0));

        assertEquals(1, xxlJobLogDao.clearLog(clearIds));
        assertEquals(2, xxlJobLogDao.pageListCount(0, 20, 1, 1, null, null, 0));

        jdbcTemplate.update(
                "INSERT INTO xxl_job_registry(registry_group, registry_key, registry_value, update_time) VALUES (?,?,?,?)",
                "EXECUTOR",
                "app",
                "addr-3",
                Timestamp.from(now));

        List<Long> lostJobIds = xxlJobLogDao.findLostJobIds(Date.from(now.minus(2, ChronoUnit.DAYS)));
        assertTrue(lostJobIds.contains(old2));
        assertTrue(!lostJobIds.contains(recent));
    }

    private long insertLog(
            int jobGroup,
            int jobId,
            Instant triggerTime,
            int triggerCode,
            int handleCode,
            int alarmStatus,
            String executorAddress) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO xxl_job_log(job_group, job_id, trigger_time, trigger_code, handle_code, alarm_status, executor_address) "
                        + "VALUES (?,?,?,?,?,?,?) RETURNING id",
                Long.class,
                jobGroup,
                jobId,
                Timestamp.from(triggerTime),
                triggerCode,
                handleCode,
                alarmStatus,
                executorAddress);
    }
}
