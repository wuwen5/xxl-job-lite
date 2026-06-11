package com.xxl.job.admin.dao;

import com.xxl.job.admin.AbstractTest;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

public class XxlJobRegistryDaoTest extends AbstractTest {
    private static final Date FIXED_NOW = new Date(1_700_000_000_000L);

    @Resource
    private XxlJobRegistryDao xxlJobRegistryDao;

    @Test
    public void test() {
        int ret = xxlJobRegistryDao.registryUpdate("g1", "k1", "v1", FIXED_NOW);
        if (ret < 1) {
            ret = xxlJobRegistryDao.registrySave("g1", "k1", "v1", FIXED_NOW);
        }

        List<XxlJobRegistry> list = xxlJobRegistryDao.findAll(1, FIXED_NOW);

        int ret2 = xxlJobRegistryDao.removeDead(Arrays.asList(1));
    }
}
