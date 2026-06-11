package com.xxl.job.admin.dao;

import com.xxl.job.admin.AbstractTest;
import com.xxl.job.admin.core.model.XxlJobLogGlue;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

public class XxlJobLogGlueDaoTest extends AbstractTest {
    private static final Date FIXED_TIME = new Date(1_700_000_000_000L);

    @Resource
    private XxlJobLogGlueDao xxlJobLogGlueDao;

    @Test
    public void test() {
        XxlJobLogGlue logGlue = new XxlJobLogGlue();
        logGlue.setJobId(1);
        logGlue.setGlueType("1");
        logGlue.setGlueSource("1");
        logGlue.setGlueRemark("1");

        logGlue.setAddTime(FIXED_TIME);
        logGlue.setUpdateTime(FIXED_TIME);
        int ret = xxlJobLogGlueDao.save(logGlue);

        List<XxlJobLogGlue> list = xxlJobLogGlueDao.findByJobId(1);

        int ret2 = xxlJobLogGlueDao.removeOld(1, 1);

        int ret3 = xxlJobLogGlueDao.deleteByJobId(1);
    }
}
