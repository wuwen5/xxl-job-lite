package com.xxl.job.admin.dao.pg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.admin.AbstractPostgreSQLTest;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class XxlJobInfoDaoPostgreSQLTest extends AbstractPostgreSQLTest {

    @Resource
    private XxlJobInfoDao xxlJobInfoDao;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE xxl_job_log, xxl_job_info, xxl_job_group RESTART IDENTITY CASCADE");
    }

    @Test
    void pageList_withDynamicFilters() {
        insertJobInfo(1, "alpha-desc", "handlerAlpha", "alice", 1, 1000L);
        insertJobInfo(1, "beta-desc", "handlerBeta", "bob", 0, 2000L);
        insertJobInfo(2, "gamma-job", "handlerAlpha", "alice2", 1, 3000L);

        List<XxlJobInfo> byGroup = xxlJobInfoDao.pageList(0, 10, 1, -1, null, null, null);
        assertEquals(2, byGroup.size());

        List<XxlJobInfo> byStatus = xxlJobInfoDao.pageList(0, 10, 0, 1, null, null, null);
        assertEquals(2, byStatus.size());

        List<XxlJobInfo> byDesc = xxlJobInfoDao.pageList(0, 10, 0, -1, "%gamma%", null, null);
        assertEquals(1, byDesc.size());

        List<XxlJobInfo> byHandler = xxlJobInfoDao.pageList(0, 10, 0, -1, null, "%handlerAlpha%", null);
        assertEquals(2, byHandler.size());

        List<XxlJobInfo> byAuthor = xxlJobInfoDao.pageList(0, 10, 0, -1, null, null, "%alice%");
        assertEquals(2, byAuthor.size());
    }

    @Test
    void pageListCount_withDynamicFilters() {
        insertJobInfo(1, "alpha-desc", "handlerAlpha", "alice", 1, 1000L);
        insertJobInfo(1, "beta-desc", "handlerBeta", "bob", 0, 2000L);
        insertJobInfo(2, "gamma-job", "handlerAlpha", "alice2", 1, 3000L);

        assertEquals(2, xxlJobInfoDao.pageListCount(1, -1, null, null, null));
        assertEquals(2, xxlJobInfoDao.pageListCount(0, 1, null, null, null));
        assertEquals(1, xxlJobInfoDao.pageListCount(0, -1, "%gamma%", null, null));
        assertEquals(2, xxlJobInfoDao.pageListCount(0, -1, null, "%handlerAlpha%", null));
        assertEquals(2, xxlJobInfoDao.pageListCount(0, -1, null, null, "%alice%"));
    }

    @Test
    void saveLoadUpdateDelete() {
        XxlJobInfo info = buildJobInfo(1, "save-desc", "handler-save", "author-save", 1, 1000L);
        assertEquals(1, xxlJobInfoDao.save(info));
        assertTrue(info.getId() > 0);

        XxlJobInfo loaded = xxlJobInfoDao.loadById(info.getId());
        assertNotNull(loaded);
        assertEquals("save-desc", loaded.getJobDesc());
        assertEquals("handler-save", loaded.getExecutorHandler());

        loaded.setJobDesc("updated-desc");
        loaded.setAuthor("updated-author");
        loaded.setExecutorHandler("updated-handler");
        loaded.setTriggerStatus(0);
        loaded.setTriggerLastTime(2000L);
        loaded.setTriggerNextTime(3000L);
        loaded.setUpdateTime(new Date());

        assertEquals(1, xxlJobInfoDao.update(loaded));
        XxlJobInfo updated = xxlJobInfoDao.loadById(info.getId());
        assertNotNull(updated);
        assertEquals("updated-desc", updated.getJobDesc());
        assertEquals("updated-author", updated.getAuthor());
        assertEquals("updated-handler", updated.getExecutorHandler());
        assertEquals(0, updated.getTriggerStatus());

        assertEquals(1, xxlJobInfoDao.delete(info.getId()));
        assertNull(xxlJobInfoDao.loadById(info.getId()));
    }

    @Test
    void scheduleJobQueryAndScheduleUpdate() {
        XxlJobInfo runningMatched = buildJobInfo(1, "running-matched", "handler1", "a1", 1, 100L);
        XxlJobInfo runningUnmatched = buildJobInfo(1, "running-unmatched", "handler2", "a2", 1, 10_000L);
        XxlJobInfo stoppedMatched = buildJobInfo(1, "stopped", "handler3", "a3", 0, 50L);
        XxlJobInfo runningForNegativeStatus = buildJobInfo(1, "running-neg", "handler4", "a4", 1, 200L);

        xxlJobInfoDao.save(runningMatched);
        xxlJobInfoDao.save(runningUnmatched);
        xxlJobInfoDao.save(stoppedMatched);
        xxlJobInfoDao.save(runningForNegativeStatus);

        List<XxlJobInfo> dueJobs = xxlJobInfoDao.scheduleJobQuery(500L, 10);
        assertEquals(2, dueJobs.size());

        XxlJobInfo updateToStop = new XxlJobInfo();
        updateToStop.setId(runningMatched.getId());
        updateToStop.setTriggerStatus(0);
        updateToStop.setTriggerLastTime(111L);
        updateToStop.setTriggerNextTime(222L);
        assertEquals(1, xxlJobInfoDao.scheduleUpdate(updateToStop));

        XxlJobInfo stoppedAfterUpdate = xxlJobInfoDao.loadById(runningMatched.getId());
        assertEquals(0, stoppedAfterUpdate.getTriggerStatus());
        assertEquals(111L, stoppedAfterUpdate.getTriggerLastTime());
        assertEquals(222L, stoppedAfterUpdate.getTriggerNextTime());

        XxlJobInfo protectedByWhere = new XxlJobInfo();
        protectedByWhere.setId(runningMatched.getId());
        protectedByWhere.setTriggerStatus(1);
        protectedByWhere.setTriggerLastTime(333L);
        protectedByWhere.setTriggerNextTime(444L);
        assertEquals(0, xxlJobInfoDao.scheduleUpdate(protectedByWhere));

        XxlJobInfo negativeStatusBranch = new XxlJobInfo();
        negativeStatusBranch.setId(runningForNegativeStatus.getId());
        negativeStatusBranch.setTriggerStatus(-1);
        negativeStatusBranch.setTriggerLastTime(777L);
        negativeStatusBranch.setTriggerNextTime(888L);
        assertEquals(1, xxlJobInfoDao.scheduleUpdate(negativeStatusBranch));

        XxlJobInfo afterNegativeStatusBranch = xxlJobInfoDao.loadById(runningForNegativeStatus.getId());
        assertEquals(1, afterNegativeStatusBranch.getTriggerStatus());
        assertEquals(777L, afterNegativeStatusBranch.getTriggerLastTime());
        assertEquals(888L, afterNegativeStatusBranch.getTriggerNextTime());
    }

    private void insertJobInfo(
            int jobGroup,
            String jobDesc,
            String executorHandler,
            String author,
            int triggerStatus,
            long triggerNextTime) {
        XxlJobInfo info = buildJobInfo(jobGroup, jobDesc, executorHandler, author, triggerStatus, triggerNextTime);
        xxlJobInfoDao.save(info);
    }

    private XxlJobInfo buildJobInfo(
            int jobGroup,
            String jobDesc,
            String executorHandler,
            String author,
            int triggerStatus,
            long triggerNextTime) {
        XxlJobInfo info = new XxlJobInfo();
        info.setJobGroup(jobGroup);
        info.setJobDesc(jobDesc);
        Date now = new Date();
        info.setAddTime(now);
        info.setUpdateTime(now);
        info.setAuthor(author);
        info.setAlarmEmail("test@example.com");
        info.setScheduleType("CRON");
        info.setScheduleConf("0/5 * * * * ?");
        info.setMisfireStrategy("DO_NOTHING");
        info.setExecutorRouteStrategy("FIRST");
        info.setExecutorHandler(executorHandler);
        info.setExecutorParam("param");
        info.setExecutorBlockStrategy("SERIAL_EXECUTION");
        info.setExecutorTimeout(0);
        info.setExecutorFailRetryCount(0);
        info.setGlueType("BEAN");
        info.setGlueSource("");
        info.setGlueRemark("remark");
        info.setGlueUpdatetime(now);
        info.setChildJobId("");
        info.setTriggerStatus(triggerStatus);
        info.setTriggerLastTime(0L);
        info.setTriggerNextTime(triggerNextTime);
        return info;
    }
}
