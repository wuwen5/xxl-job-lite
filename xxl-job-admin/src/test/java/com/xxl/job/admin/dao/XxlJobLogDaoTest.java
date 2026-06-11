package com.xxl.job.admin.dao;

import com.xxl.job.admin.AbstractTest;
import com.xxl.job.admin.core.model.XxlJobLog;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

public class XxlJobLogDaoTest extends AbstractTest {
    private static final Date FIXED_TRIGGER_TIME = new Date(1_700_000_000_000L);
    private static final Date FIXED_HANDLE_TIME = new Date(1_700_000_100_000L);
    private static final Date FIXED_CLEAR_BEFORE_TIME = new Date(1_700_000_200_000L);

    @Resource
    private XxlJobLogDao xxlJobLogDao;

    @Test
    public void test() {
        List<XxlJobLog> list = xxlJobLogDao.pageList(0, 10, 1, 1, null, null, 1);
        int list_count = xxlJobLogDao.pageListCount(0, 10, 1, 1, null, null, 1);

        XxlJobLog log = new XxlJobLog();
        log.setJobGroup(1);
        log.setJobId(1);

        long ret1 = xxlJobLogDao.save(log);
        XxlJobLog dto = xxlJobLogDao.load(log.getId());

        log.setTriggerTime(FIXED_TRIGGER_TIME);
        log.setTriggerCode(1);
        log.setTriggerMsg("1");
        log.setExecutorAddress("1");
        log.setExecutorHandler("1");
        log.setExecutorParam("1");
        ret1 = xxlJobLogDao.updateTriggerInfo(log);
        dto = xxlJobLogDao.load(log.getId());

        log.setHandleTime(FIXED_HANDLE_TIME);
        log.setHandleCode(2);
        log.setHandleMsg("2");
        ret1 = xxlJobLogDao.updateHandleInfo(log);
        dto = xxlJobLogDao.load(log.getId());

        List<Long> ret4 = xxlJobLogDao.findClearLogIds(1, 1, FIXED_CLEAR_BEFORE_TIME, 100, 100);

        int ret2 = xxlJobLogDao.delete(log.getJobId());
    }
}
