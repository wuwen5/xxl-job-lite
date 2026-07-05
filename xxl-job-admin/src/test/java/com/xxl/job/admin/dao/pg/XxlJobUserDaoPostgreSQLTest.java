package com.xxl.job.admin.dao.pg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.xxl.job.admin.AbstractPostgreSQLTest;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.dao.XxlJobUserDao;
import jakarta.annotation.Resource;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class XxlJobUserDaoPostgreSQLTest extends AbstractPostgreSQLTest {

    @Resource
    private XxlJobUserDao xxlJobUserDao;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE xxl_job_user RESTART IDENTITY CASCADE");
    }

    @Test
    void saveLoadUpdateDeleteAndPageListCount() {
        XxlJobUser user = new XxlJobUser();
        user.setUsername("alpha");
        user.setPassword("p1");
        user.setRole(0);
        user.setPermission("1,2");

        assertEquals(1, xxlJobUserDao.save(user));
        XxlJobUser loaded = xxlJobUserDao.loadByUserName("alpha");
        assertNotNull(loaded);
        assertEquals("p1", loaded.getPassword());

        loaded.setPassword("p2");
        loaded.setRole(1);
        loaded.setPermission(null);
        assertEquals(1, xxlJobUserDao.update(loaded));

        XxlJobUser updated = xxlJobUserDao.loadByUserName("alpha");
        assertNotNull(updated);
        assertEquals("p2", updated.getPassword());
        assertEquals(1, updated.getRole());

        updated.setPassword("");
        updated.setRole(0);
        updated.setPermission("1");
        assertEquals(1, xxlJobUserDao.update(updated));

        XxlJobUser noPasswordUpdate = xxlJobUserDao.loadByUserName("alpha");
        assertNotNull(noPasswordUpdate);
        assertEquals("p2", noPasswordUpdate.getPassword());
        assertEquals(0, noPasswordUpdate.getRole());

        XxlJobUser user2 = new XxlJobUser();
        user2.setUsername("beta");
        user2.setPassword("p3");
        user2.setRole(1);
        user2.setPermission(null);
        assertEquals(1, xxlJobUserDao.save(user2));

        assertEquals(2, xxlJobUserDao.pageListCount(0, 10, null, -1));
        assertEquals(1, xxlJobUserDao.pageListCount(0, 10, "%alp%", -1));
        assertEquals(1, xxlJobUserDao.pageListCount(0, 10, null, 1));

        assertEquals(1, xxlJobUserDao.delete(user.getId()));
        assertNull(xxlJobUserDao.loadByUserName("alpha"));
    }

    @Test
    void pageList_withFilters() {
        saveUser("alpha", 0);
        saveUser("beta", 1);

        List<XxlJobUser> users = xxlJobUserDao.pageList(0, 10, "%alpha%", -1);
        assertEquals(1, users.size());
    }

    private void saveUser(String username, int role) {
        XxlJobUser user = new XxlJobUser();
        user.setUsername(username);
        user.setPassword("pwd");
        user.setRole(role);
        user.setPermission(null);
        xxlJobUserDao.save(user);
    }
}
