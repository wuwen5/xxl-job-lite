package com.xxl.job.admin.dao.pg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.admin.AbstractPostgreSQLTest;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.admin.dao.XxlJobRegistryDao;
import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class XxlJobRegistryDaoPostgreSQLTest extends AbstractPostgreSQLTest {
    private static final Instant BASE_INSTANT = Instant.parse("2024-01-01T00:00:00Z");

    @Resource
    private XxlJobRegistryDao xxlJobRegistryDao;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE xxl_job_registry RESTART IDENTITY CASCADE");
    }

    @Test
    void findDead() {
        Instant now = BASE_INSTANT;
        insertRegistry("EXECUTOR", "app", "dead-address", now.minus(2, ChronoUnit.MINUTES));
        List<Integer> deadIds = xxlJobRegistryDao.findDead(60, Date.from(now));
        assertEquals(1, deadIds.size());
    }

    @Test
    void findAllRegistryUpdateSaveDeleteAndRemoveDead() {
        Instant now = BASE_INSTANT.plus(1, ChronoUnit.HOURS);

        assertEquals(0, xxlJobRegistryDao.registryUpdate("EXECUTOR", "app", "address-1", Date.from(now)));
        assertEquals(
                1,
                xxlJobRegistryDao.registrySave(
                        "EXECUTOR", "app", "address-1", Date.from(now.minus(120, ChronoUnit.SECONDS))));
        assertEquals(
                1,
                xxlJobRegistryDao.registrySave(
                        "EXECUTOR", "app", "address-2", Date.from(now.minus(10, ChronoUnit.SECONDS))));

        assertEquals(1, xxlJobRegistryDao.registryUpdate("EXECUTOR", "app", "address-1", Date.from(now)));

        List<XxlJobRegistry> alive = xxlJobRegistryDao.findAll(60, Date.from(now));
        assertEquals(2, alive.size());
        assertTrue(alive.stream().anyMatch(item -> "address-1".equals(item.getRegistryValue())));
        assertTrue(alive.stream().anyMatch(item -> "address-2".equals(item.getRegistryValue())));

        int staleId = insertRegistry("EXECUTOR", "app", "stale-address", now.minus(10, ChronoUnit.MINUTES));
        assertEquals(1, xxlJobRegistryDao.removeDead(List.of(staleId)));

        Integer staleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM xxl_job_registry WHERE registry_value = 'stale-address'", Integer.class);
        assertEquals(0, staleCount);

        assertEquals(1, xxlJobRegistryDao.registryDelete("EXECUTOR", "app", "address-2"));
        List<XxlJobRegistry> aliveAfterDelete = xxlJobRegistryDao.findAll(60, Date.from(now));
        assertEquals(1, aliveAfterDelete.size());
        assertEquals("address-1", aliveAfterDelete.get(0).getRegistryValue());
    }

    private int insertRegistry(String group, String key, String value, Instant updateTime) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO xxl_job_registry(registry_group, registry_key, registry_value, update_time) VALUES (?,?,?,?) RETURNING id",
                Integer.class,
                group,
                key,
                value,
                Timestamp.from(updateTime));
    }
}
