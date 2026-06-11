package com.xxl.job.admin.dao.pg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.admin.AbstractPostgreSQLTest;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class XxlJobGroupDaoPostgreSQLTest extends AbstractPostgreSQLTest {
    private static final Date FIXED_UPDATE_TIME = new Date(1_700_000_000_000L);
    private static final Date FIXED_SECOND_UPDATE_TIME = new Date(1_700_000_100_000L);

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE xxl_job_group RESTART IDENTITY CASCADE");
    }

    @Test
    void saveLoadUpdateRemoveAndFindMethods() {
        XxlJobGroup group = new XxlJobGroup();
        group.setAppname("app-a");
        group.setTitle("group-a");
        group.setAddressType(0);
        group.setAddressList("127.0.0.1:9999");
        group.setUpdateTime(FIXED_UPDATE_TIME);

        assertEquals(1, xxlJobGroupDao.save(group));
        assertTrue(group.getId() > 0);

        XxlJobGroup loaded = xxlJobGroupDao.load(group.getId());
        assertNotNull(loaded);
        assertEquals("app-a", loaded.getAppname());

        loaded.setAppname("app-b");
        loaded.setTitle("group-b");
        loaded.setAddressType(1);
        loaded.setAddressList("127.0.0.1:10000");
        loaded.setUpdateTime(FIXED_SECOND_UPDATE_TIME);

        assertEquals(1, xxlJobGroupDao.update(loaded));
        XxlJobGroup updated = xxlJobGroupDao.load(group.getId());
        assertNotNull(updated);
        assertEquals("app-b", updated.getAppname());
        assertEquals(1, updated.getAddressType());

        List<XxlJobGroup> all = xxlJobGroupDao.findAll();
        assertEquals(1, all.size());

        List<XxlJobGroup> byAddressType = xxlJobGroupDao.findByAddressType(1);
        assertEquals(1, byAddressType.size());

        assertEquals(1, xxlJobGroupDao.remove(group.getId()));
        assertNull(xxlJobGroupDao.load(group.getId()));
    }

    @Test
    void pageListCount_withDynamicFilters() {
        saveGroup("executor-alpha", "title-1", 0);
        saveGroup("executor-beta", "title-2", 0);
        saveGroup("misc", "other-title", 1);

        assertEquals(2, xxlJobGroupDao.pageListCount(0, 10, "executor", null));
        assertEquals(3, xxlJobGroupDao.pageListCount(0, 10, null, "title"));
        assertEquals(1, xxlJobGroupDao.pageListCount(0, 10, "alpha", "title-1"));
    }

    @Test
    void pageList_withDynamicFilters() {
        saveGroup("executor-alpha", "title-1", 0);
        saveGroup("executor-beta", "title-2", 0);

        List<XxlJobGroup> result = xxlJobGroupDao.pageList(0, 10, "executor", "title");
        assertEquals(2, result.size());
    }

    private void saveGroup(String appName, String title, int addressType) {
        XxlJobGroup group = new XxlJobGroup();
        group.setAppname(appName);
        group.setTitle(title);
        group.setAddressType(addressType);
        group.setAddressList("127.0.0.1:9999");
        group.setUpdateTime(FIXED_UPDATE_TIME);
        xxlJobGroupDao.save(group);
    }
}
