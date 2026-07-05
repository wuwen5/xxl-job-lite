package com.xxl.job.admin.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.admin.AbstractTest;
import com.xxl.job.admin.core.model.XxlJobLog;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class XxlJobLogDaoTest extends AbstractTest {
    private static final Date FIXED_TRIGGER_TIME = new Date(1_700_000_000_000L);
    private static final Date FIXED_HANDLE_TIME = new Date(1_700_000_100_000L);
    private static final Date FIXED_CLEAR_BEFORE_TIME = new Date(1_700_000_200_000L);

    @Resource
    private XxlJobLogDao xxlJobLogDao;

    @BeforeEach
    void setUp() {
        xxlJobLogDao.delete(1);
    }

    @Test
    public void test() {
        List<XxlJobLog> list = xxlJobLogDao.pageList(0, 10, 1, 1, null, null, 1);
        int list_count = xxlJobLogDao.pageListCount(0, 10, 1, 1, null, null, 1);
        assertTrue(list_count >= list.size());

        XxlJobLog log = new XxlJobLog();
        log.setJobGroup(1);
        log.setJobId(1);

        long ret1 = xxlJobLogDao.save(log);
        assertEquals(1L, ret1);
        XxlJobLog dto = xxlJobLogDao.load(log.getId());
        assertNotNull(dto);
        assertEquals(log.getId(), dto.getId());

        log.setTriggerTime(FIXED_TRIGGER_TIME);
        log.setTriggerCode(1);
        log.setTriggerMsg("1");
        log.setExecutorAddress("1");
        log.setExecutorHandler("1");
        log.setExecutorParam("1");
        ret1 = xxlJobLogDao.updateTriggerInfo(log);
        assertEquals(1L, ret1);
        dto = xxlJobLogDao.load(log.getId());
        assertNotNull(dto);
        assertEquals(1, dto.getTriggerCode());
        assertEquals("1", dto.getExecutorHandler());

        log.setHandleTime(FIXED_HANDLE_TIME);
        log.setHandleCode(2);
        log.setHandleMsg("2");
        ret1 = xxlJobLogDao.updateHandleInfo(log);
        assertEquals(1L, ret1);
        dto = xxlJobLogDao.load(log.getId());
        assertNotNull(dto);
        assertEquals(2, dto.getHandleCode());

        List<Long> clearLogIds = xxlJobLogDao.findClearLogIds(1, 1, FIXED_CLEAR_BEFORE_TIME, 100, 100);
        assertNotNull(clearLogIds);

        int ret2 = xxlJobLogDao.delete(log.getJobId());
        assertEquals(1, ret2);

        xxlJobLogDao.findFailJobLogIds(10);
    }
}
