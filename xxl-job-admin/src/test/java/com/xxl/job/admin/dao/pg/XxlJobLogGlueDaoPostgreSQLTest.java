package com.xxl.job.admin.dao.pg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.admin.AbstractPostgreSQLTest;
import com.xxl.job.admin.core.model.XxlJobLogGlue;
import com.xxl.job.admin.dao.XxlJobLogGlueDao;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class XxlJobLogGlueDaoPostgreSQLTest extends AbstractPostgreSQLTest {
    private static final long BASE_TIME_MILLIS = 1_700_000_000_000L;

    @Resource
    private XxlJobLogGlueDao xxlJobLogGlueDao;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE xxl_job_logglue RESTART IDENTITY CASCADE");
    }

    @Test
    void saveFindRemoveOldAndDeleteByJobId() {
        saveLogGlue(1, "first", new Date(BASE_TIME_MILLIS - 10_000));
        int secondId = saveLogGlue(1, "second", new Date(BASE_TIME_MILLIS - 5_000));
        saveLogGlue(2, "other-job", new Date(BASE_TIME_MILLIS));

        List<XxlJobLogGlue> byJobId = xxlJobLogGlueDao.findByJobId(1);
        assertEquals(2, byJobId.size());
        assertEquals(secondId, byJobId.get(0).getId());

        assertEquals(1, xxlJobLogGlueDao.removeOld(1, 1));
        List<XxlJobLogGlue> afterRemoveOld = xxlJobLogGlueDao.findByJobId(1);
        assertEquals(1, afterRemoveOld.size());
        assertEquals(secondId, afterRemoveOld.get(0).getId());

        assertEquals(1, xxlJobLogGlueDao.deleteByJobId(1));
        assertTrue(xxlJobLogGlueDao.findByJobId(1).isEmpty());
    }

    private int saveLogGlue(int jobId, String remark, Date time) {
        XxlJobLogGlue logGlue = new XxlJobLogGlue();
        logGlue.setJobId(jobId);
        logGlue.setGlueType("BEAN");
        logGlue.setGlueSource("source-" + remark);
        logGlue.setGlueRemark(remark);
        logGlue.setAddTime(time);
        logGlue.setUpdateTime(time);
        assertEquals(1, xxlJobLogGlueDao.save(logGlue));
        return logGlue.getId();
    }
}
