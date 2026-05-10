package com.xxl.job.admin.dao.pg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.xxl.job.admin.AbstractPostgreSQLTest;
import com.xxl.job.admin.core.model.XxlJobLogReport;
import com.xxl.job.admin.dao.XxlJobLogReportDao;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class XxlJobLogReportDaoPostgreSQLTest extends AbstractPostgreSQLTest {

    @Resource
    private XxlJobLogReportDao xxlJobLogReportDao;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("TRUNCATE TABLE xxl_job_log_report RESTART IDENTITY CASCADE");
    }

    @Test
    void saveUpdateQueryAndTotal() {
        Date day1 = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
        Date day2 = new Date();

        XxlJobLogReport report1 = new XxlJobLogReport();
        report1.setTriggerDay(day1);
        report1.setRunningCount(1);
        report1.setSucCount(2);
        report1.setFailCount(3);
        assertEquals(1, xxlJobLogReportDao.save(report1));

        XxlJobLogReport report2 = new XxlJobLogReport();
        report2.setTriggerDay(day2);
        report2.setRunningCount(4);
        report2.setSucCount(5);
        report2.setFailCount(6);
        assertEquals(1, xxlJobLogReportDao.save(report2));

        report2.setRunningCount(7);
        report2.setSucCount(8);
        report2.setFailCount(9);
        assertEquals(1, xxlJobLogReportDao.update(report2));

        List<XxlJobLogReport> reports =
                xxlJobLogReportDao.queryLogReport(new Date(day1.getTime() - 1_000), new Date(day2.getTime() + 1_000));
        assertEquals(2, reports.size());

        XxlJobLogReport total = xxlJobLogReportDao.queryLogReportTotal();
        assertNotNull(total);
        assertEquals(8, total.getRunningCount());
        assertEquals(10, total.getSucCount());
        assertEquals(12, total.getFailCount());
    }
}
