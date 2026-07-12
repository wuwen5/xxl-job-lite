package com.xxl.job.admin.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.xxl.job.admin.AbstractTest;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLogReport;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.dao.*;
import com.xxl.job.core.biz.model.ReturnT;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Unit tests for XxlJobServiceImpl.
 */
public class XxlJobServiceImplTest extends AbstractTest {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobServiceImplTest.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    XxlJobServiceImpl xxlJobService;

    @Autowired
    XxlJobGroupDao xxlJobGroupDao;

    @Autowired
    XxlJobInfoDao xxlJobInfoDao;

    @Autowired
    XxlJobLogDao xxlJobLogDao;

    @Autowired
    XxlJobLogGlueDao xxlJobLogGlueDao;

    @Autowired
    XxlJobLogReportDao xxlJobLogReportDao;

    private XxlJobUser adminUser;
    private XxlJobUser normalUser;

    @BeforeEach
    public void setUp() {
        // Clean up test data
        jdbcTemplate.execute("DELETE FROM xxl_job_log_report WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_logglue WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_log WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_info WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_group WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_user WHERE id > 0");

        // Insert test job groups
        jdbcTemplate.execute("INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time) "
                + "VALUES (1, 'test-executor-1', 'Test Executor 1', 0, NULL, NOW())");
        jdbcTemplate.execute("INSERT INTO xxl_job_group(id, app_name, title, address_type, address_list, update_time) "
                + "VALUES (2, 'test-executor-2', 'Test Executor 2', 0, NULL, NOW())");

        // Insert test users
        jdbcTemplate.execute("INSERT INTO xxl_job_user(id, username, password, role, permission) "
                + "VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL)");
        jdbcTemplate.execute("INSERT INTO xxl_job_user(id, username, password, role, permission) "
                + "VALUES (2, 'normal', 'e10adc3949ba59abbe56e057f20f883e', 0, '1')");

        // Create user objects
        adminUser = new XxlJobUser();
        adminUser.setId(1);
        adminUser.setUsername("admin");
        adminUser.setRole(1);

        normalUser = new XxlJobUser();
        normalUser.setId(2);
        normalUser.setUsername("normal");
        normalUser.setRole(0);
        normalUser.setPermission("1");
    }

    @AfterEach
    public void tearDown() {
        // Clean up test data
        jdbcTemplate.execute("DELETE FROM xxl_job_log_report WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_logglue WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_log WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_info WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_group WHERE id > 0");
        jdbcTemplate.execute("DELETE FROM xxl_job_user WHERE id > 0");
    }

    // ---------------------- pageList ----------------------

    /**
     * Test pageList with no filters
     */
    @Test
    public void testPageListNoFilters() {
        Map<String, Object> result = xxlJobService.pageList(0, 10, -1, -1, "", "", "");

        assertNotNull(result);
        assertTrue(result.containsKey("recordsTotal"));
        assertTrue(result.containsKey("recordsFiltered"));
        assertTrue(result.containsKey("data"));
        assertEquals(0L, ((Number) result.get("recordsTotal")).longValue());
        logger.info("testPageListNoFilters: recordsTotal={}", result.get("recordsTotal"));
    }

    /**
     * Test pageList with job group filter
     */
    @Test
    public void testPageListWithJobGroupFilter() {
        // Insert a test job
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setTriggerStatus(0);
        xxlJobInfoDao.save(jobInfo);

        Map<String, Object> result = xxlJobService.pageList(0, 10, 1, -1, "", "", "");

        assertNotNull(result);
        assertEquals(1L, ((Number) result.get("recordsTotal")).longValue());
        logger.info("testPageListWithJobGroupFilter: recordsTotal={}", result.get("recordsTotal"));
    }

    /**
     * Test pageList with trigger status filter
     */
    @Test
    public void testPageListWithTriggerStatusFilter() {
        // Insert a running job
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Running Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("0 0 12 * * ? *");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setTriggerStatus(1);
        jobInfo.setTriggerLastTime(0);
        jobInfo.setTriggerNextTime(System.currentTimeMillis() + 86400000);
        xxlJobInfoDao.save(jobInfo);

        Map<String, Object> result = xxlJobService.pageList(0, 10, -1, 1, "", "", "");

        assertNotNull(result);
        assertEquals(1L, ((Number) result.get("recordsTotal")).longValue());
        logger.info("testPageListWithTriggerStatusFilter: recordsTotal={}", result.get("recordsTotal"));
    }

    // ---------------------- add ----------------------

    /**
     * Test add with valid CRON job
     */
    @Test
    public void testAddCronJobSuccess() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test CRON Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("0 0 12 * * ? *");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setExecutorTimeout(0);
        jobInfo.setExecutorFailRetryCount(0);
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertNotNull(result.getContent());
        logger.info("testAddCronJobSuccess: jobId={}", result.getContent());
    }

    /**
     * Test add with valid FIX_RATE job
     */
    @Test
    public void testAddFixRateJobSuccess() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test FIX_RATE Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("FIX_RATE");
        jobInfo.setScheduleConf("60");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setExecutorTimeout(0);
        jobInfo.setExecutorFailRetryCount(0);
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        logger.info("testAddFixRateJobSuccess: jobId={}", result.getContent());
    }

    /**
     * Test add with invalid job group → FAIL
     */
    @Test
    public void testAddInvalidJobGroup() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(999); // Non-existent
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testAddInvalidJobGroup: msg={}", result.getMsg());
    }

    /**
     * Test add with empty job description → FAIL
     */
    @Test
    public void testAddEmptyJobDesc() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testAddEmptyJobDesc: msg={}", result.getMsg());
    }

    /**
     * Test add with empty author → FAIL
     */
    @Test
    public void testAddEmptyAuthor() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testAddEmptyAuthor: msg={}", result.getMsg());
    }

    /**
     * Test add with invalid schedule type → FAIL
     */
    @Test
    public void testAddInvalidScheduleType() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("INVALID");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testAddInvalidScheduleType: msg={}", result.getMsg());
    }

    /**
     * Test add with invalid CRON expression → FAIL
     */
    @Test
    public void testAddInvalidCronExpression() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("invalid cron");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testAddInvalidCronExpression: msg={}", result.getMsg());
    }

    /**
     * Test add with invalid FIX_RATE value → FAIL
     */
    @Test
    public void testAddInvalidFixRateValue() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("FIX_RATE");
        jobInfo.setScheduleConf("0"); // Must be >= 1
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testAddInvalidFixRateValue: msg={}", result.getMsg());
    }

    /**
     * Test add with non-numeric FIX_RATE value → FAIL
     */
    @Test
    public void testAddNonNumericFixRateValue() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("FIX_RATE");
        jobInfo.setScheduleConf("abc");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testAddNonNumericFixRateValue: msg={}", result.getMsg());
    }

    /**
     * Test add with BEAN glue type but no executor handler → FAIL
     */
    @Test
    public void testAddBeanWithoutHandler() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testAddBeanWithoutHandler: msg={}", result.getMsg());
    }

    /**
     * Test add with SHELL glue type → should remove \r from glue source
     */
    @Test
    public void testAddShellGlueTypeRemovesCarriageReturn() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Shell Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("GLUE_SHELL");
        jobInfo.setGlueSource("echo 'test'\r\necho 'hello'\r");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        // Verify \r was removed
        XxlJobInfo savedJob = xxlJobInfoDao.loadById(Integer.parseInt(result.getContent()));
        assertFalse(savedJob.getGlueSource().contains("\r"));
        logger.info("testAddShellGlueTypeRemovesCarriageReturn: glueSource={}", savedJob.getGlueSource());
    }

    /**
     * Test add with invalid child job ID → FAIL
     */
    @Test
    public void testAddWithInvalidChildJobId() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setChildJobId("999"); // Non-existent child job

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testAddWithInvalidChildJobId: msg={}", result.getMsg());
    }

    /**
     * Test add with valid child job ID
     */
    @Test
    public void testAddWithValidChildJobId() {
        // Insert child job first
        XxlJobInfo childJob = new XxlJobInfo();
        childJob.setJobGroup(1);
        childJob.setJobDesc("Child Job");
        childJob.setAuthor("tester");
        childJob.setScheduleType("NONE");
        childJob.setMisfireStrategy("DO_NOTHING");
        childJob.setExecutorRouteStrategy("FIRST");
        childJob.setExecutorHandler("childJobHandler");
        childJob.setExecutorBlockStrategy("SERIAL_EXECUTION");
        childJob.setGlueType("BEAN");
        xxlJobInfoDao.save(childJob);

        // Insert parent job with child job ID
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Parent Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setChildJobId(String.valueOf(childJob.getId()));

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        logger.info("testAddWithValidChildJobId: jobId={}", result.getContent());
    }

    /**
     * Test add with non-numeric child job ID → FAIL
     */
    @Test
    public void testAddWithNonNumericChildJobId() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setChildJobId("abc");

        ReturnT<String> result = xxlJobService.add(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testAddWithNonNumericChildJobId: msg={}", result.getMsg());
    }

    // ---------------------- update ----------------------

    /**
     * Test update with valid data
     */
    @Test
    public void testUpdateSuccess() {
        // Insert a job first
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Original Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("0 0 12 * * ? *");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        xxlJobInfoDao.save(jobInfo);

        // Update the job
        jobInfo.setJobDesc("Updated Job");
        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        logger.info("testUpdateSuccess: result={}", result.getMsg());
    }

    /**
     * Test update with non-existent job → FAIL
     */
    @Test
    public void testUpdateNonExistentJob() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(9999);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateNonExistentJob: msg={}", result.getMsg());
    }

    /**
     * Test update with self-referencing child job → FAIL
     */
    @Test
    public void testUpdateSelfReferencingChildJob() {
        // Insert a job
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        xxlJobInfoDao.save(jobInfo);

        // Try to set itself as child job
        jobInfo.setChildJobId(String.valueOf(jobInfo.getId()));
        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateSelfReferencingChildJob: msg={}", result.getMsg());
    }

    /**
     * Test update with empty job description -> FAIL
     */
    @Test
    public void testUpdateEmptyJobDesc() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(1);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateEmptyJobDesc: msg={}", result.getMsg());
    }

    /**
     * Test update with empty author -> FAIL
     */
    @Test
    public void testUpdateEmptyAuthor() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(1);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateEmptyAuthor: msg={}", result.getMsg());
    }

    /**
     * Test update with invalid schedule type -> FAIL
     */
    @Test
    public void testUpdateInvalidScheduleType() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(1);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("INVALID");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateInvalidScheduleType: msg={}", result.getMsg());
    }

    /**
     * Test update with invalid CRON expression -> FAIL
     */
    @Test
    public void testUpdateInvalidCronExpression() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(1);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("not a cron");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateInvalidCronExpression: msg={}", result.getMsg());
    }

    /**
     * Test update with valid FIX_RATE schedule -> SUCCESS
     */
    @Test
    public void testUpdateFixRateJobSuccess() {
        // Insert a job with NONE schedule first
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test FIX_RATE Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        xxlJobInfoDao.save(jobInfo);

        // Update to FIX_RATE
        jobInfo.setScheduleType("FIX_RATE");
        jobInfo.setScheduleConf("60");
        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        logger.info("testUpdateFixRateJobSuccess: msg={}", result.getMsg());
    }

    /**
     * Test update with FIX_RATE but null schedule conf -> FAIL
     */
    @Test
    public void testUpdateFixRateNullConf() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(1);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("FIX_RATE");
        jobInfo.setScheduleConf(null);
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateFixRateNullConf: msg={}", result.getMsg());
    }

    /**
     * Test update with FIX_RATE and value 0 -> FAIL
     */
    @Test
    public void testUpdateFixRateZeroValue() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(1);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("FIX_RATE");
        jobInfo.setScheduleConf("0");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateFixRateZeroValue: msg={}", result.getMsg());
    }

    /**
     * Test update with FIX_RATE and non-numeric value -> FAIL
     */
    @Test
    public void testUpdateFixRateNonNumeric() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(1);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("FIX_RATE");
        jobInfo.setScheduleConf("abc");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateFixRateNonNumeric: msg={}", result.getMsg());
    }

    /**
     * Test update with invalid executor route strategy -> FAIL
     */
    @Test
    public void testUpdateInvalidExecutorRouteStrategy() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(1);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("INVALID");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateInvalidExecutorRouteStrategy: msg={}", result.getMsg());
    }

    /**
     * Test update with invalid misfire strategy -> FAIL
     */
    @Test
    public void testUpdateInvalidMisfireStrategy() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(1);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("INVALID");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateInvalidMisfireStrategy: msg={}", result.getMsg());
    }

    /**
     * Test update with invalid executor block strategy -> FAIL
     */
    @Test
    public void testUpdateInvalidExecutorBlockStrategy() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(1);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("INVALID");
        jobInfo.setGlueType("BEAN");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateInvalidExecutorBlockStrategy: msg={}", result.getMsg());
    }

    /**
     * Test update with child job id pointing to non-existent job -> FAIL
     */
    @Test
    public void testUpdateChildJobNotFound() {
        // Insert a parent job
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Parent Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        xxlJobInfoDao.save(jobInfo);

        // Update with non-existent child job id
        jobInfo.setChildJobId("9999");
        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateChildJobNotFound: msg={}", result.getMsg());
    }

    /**
     * Test update with child job in a group the login user has no permission for -> FAIL
     */
    @Test
    public void testUpdateChildJobPermissionDenied() {
        // Insert a child job in group 2 (normalUser only has permission for group 1)
        XxlJobInfo childJob = new XxlJobInfo();
        childJob.setJobGroup(2);
        childJob.setJobDesc("Child Job");
        childJob.setAuthor("tester");
        childJob.setScheduleType("NONE");
        childJob.setMisfireStrategy("DO_NOTHING");
        childJob.setExecutorRouteStrategy("FIRST");
        childJob.setExecutorHandler("childJobHandler");
        childJob.setExecutorBlockStrategy("SERIAL_EXECUTION");
        childJob.setGlueType("BEAN");
        xxlJobInfoDao.save(childJob);

        // Insert a parent job in group 1
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Parent Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        xxlJobInfoDao.save(jobInfo);

        // Update with child job id pointing to group-2 child; normalUser has no permission
        jobInfo.setChildJobId(String.valueOf(childJob.getId()));
        ReturnT<String> result = xxlJobService.update(jobInfo, normalUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateChildJobPermissionDenied: msg={}", result.getMsg());
    }

    /**
     * Test update with non-numeric child job id -> FAIL
     */
    @Test
    public void testUpdateNonNumericChildJobId() {
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setId(1);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setChildJobId("abc");

        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateNonNumericChildJobId: msg={}", result.getMsg());
    }

    /**
     * Test update with non-existent job group -> FAIL
     */
    @Test
    public void testUpdateInvalidJobGroup() {
        // Insert a job first
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Test Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        xxlJobInfoDao.save(jobInfo);

        // Update with non-existent job group
        jobInfo.setJobGroup(999);
        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateInvalidJobGroup: msg={}", result.getMsg());
    }

    /**
     * Test update when trigger status is 1 and schedule is unchanged ->
     * existing trigger next time is reused (no regeneration).
     */
    @Test
    public void testUpdateTriggerStatus1ScheduleUnchanged() {
        // Insert a running job with CRON schedule
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Running Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("0 0 12 * * ? *");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setTriggerStatus(1);
        jobInfo.setTriggerNextTime(123456789L);
        xxlJobInfoDao.save(jobInfo);

        // Update only jobDesc; schedule data unchanged
        jobInfo.setJobDesc("Renamed Running Job");
        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        XxlJobInfo updated = xxlJobInfoDao.loadById(jobInfo.getId());
        assertEquals(123456789L, updated.getTriggerNextTime());
        logger.info("testUpdateTriggerStatus1ScheduleUnchanged: triggerNextTime unchanged");
    }

    /**
     * Test update when trigger status is 1 and CRON schedule is changed ->
     * next trigger time is regenerated from new cron expression.
     */
    @Test
    public void testUpdateTriggerStatus1ScheduleChanged() {
        // Insert a running job with CRON schedule
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Running Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("0 0 12 * * ? *");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setTriggerStatus(1);
        jobInfo.setTriggerNextTime(123456789L);
        xxlJobInfoDao.save(jobInfo);

        // Change CRON expression -> regenerate trigger next time
        jobInfo.setScheduleConf("0 0 18 * * ? *");
        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        XxlJobInfo updated = xxlJobInfoDao.loadById(jobInfo.getId());
        assertNotEquals(123456789L, updated.getTriggerNextTime());
        logger.info(
                "testUpdateTriggerStatus1ScheduleChanged: triggerNextTime regenerated to {}",
                updated.getTriggerNextTime());
    }

    /**
     * Test update when trigger status is 1 and schedule changes to NONE ->
     * generateNextValidTime returns null -> FAIL.
     */
    @Test
    public void testUpdateTriggerStatus1ScheduleChangedToNone() {
        // Insert a running job with CRON schedule
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Running Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("0 0 12 * * ? *");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setTriggerStatus(1);
        jobInfo.setTriggerNextTime(123456789L);
        xxlJobInfoDao.save(jobInfo);

        // Change schedule type to NONE -> generateNextValidTime returns null -> FAIL
        jobInfo.setScheduleType("NONE");
        ReturnT<String> result = xxlJobService.update(jobInfo, adminUser);

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testUpdateTriggerStatus1ScheduleChangedToNone: msg={}", result.getMsg());
    }
    // ---------------------- remove ----------------------

    /**
     * Test remove existing job
     */
    @Test
    public void testRemoveExistingJob() {
        // Insert a job
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Job to Remove");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        xxlJobInfoDao.save(jobInfo);

        ReturnT<String> result = xxlJobService.remove(jobInfo.getId());

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        // Verify job was deleted
        assertNull(xxlJobInfoDao.loadById(jobInfo.getId()));
        logger.info("testRemoveExistingJob: jobId={}", jobInfo.getId());
    }

    /**
     * Test remove non-existent job → returns SUCCESS (idempotent)
     */
    @Test
    public void testRemoveNonExistentJob() {
        ReturnT<String> result = xxlJobService.remove(9999);

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        logger.info("testRemoveNonExistentJob: result=SUCCESS (idempotent)");
    }

    // ---------------------- start ----------------------

    /**
     * Test start a stopped job
     */
    @Test
    public void testStartStoppedJob() {
        // Insert a stopped job
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Job to Start");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("0 0 12 * * ? *");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setTriggerStatus(0);
        xxlJobInfoDao.save(jobInfo);

        ReturnT<String> result = xxlJobService.start(jobInfo.getId());

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        // Verify job is now running
        XxlJobInfo updatedJob = xxlJobInfoDao.loadById(jobInfo.getId());
        assertEquals(1, updatedJob.getTriggerStatus());
        logger.info("testStartStoppedJob: triggerStatus={}", updatedJob.getTriggerStatus());
    }

    /**
     * Test start a NONE schedule type job → FAIL
     */
    @Test
    public void testStartNoneScheduleType() {
        // Insert a NONE schedule job
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("NONE Schedule Job");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setTriggerStatus(0);
        xxlJobInfoDao.save(jobInfo);

        ReturnT<String> result = xxlJobService.start(jobInfo.getId());

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testStartNoneScheduleType: msg={}", result.getMsg());
    }

    // ---------------------- stop ----------------------

    /**
     * Test stop a running job
     */
    @Test
    public void testStopRunningJob() {
        // Insert a running job
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Job to Stop");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("CRON");
        jobInfo.setScheduleConf("0 0 12 * * ? *");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        jobInfo.setTriggerStatus(1);
        jobInfo.setTriggerLastTime(0);
        jobInfo.setTriggerNextTime(System.currentTimeMillis() + 86400000);
        xxlJobInfoDao.save(jobInfo);

        ReturnT<String> result = xxlJobService.stop(jobInfo.getId());

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        // Verify job is now stopped
        XxlJobInfo updatedJob = xxlJobInfoDao.loadById(jobInfo.getId());
        assertEquals(0, updatedJob.getTriggerStatus());
        logger.info("testStopRunningJob: triggerStatus={}", updatedJob.getTriggerStatus());
    }

    // ---------------------- trigger ----------------------

    /**
     * Test trigger with null login user → FAIL
     */
    @Test
    public void testTriggerNullLoginUser() {
        ReturnT<String> result = xxlJobService.trigger(null, 1, "", "");

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testTriggerNullLoginUser: msg={}", result.getMsg());
    }

    /**
     * Test trigger with non-existent job → FAIL
     */
    @Test
    public void testTriggerNonExistentJob() {
        ReturnT<String> result = xxlJobService.trigger(adminUser, 9999, "", "");

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testTriggerNonExistentJob: msg={}", result.getMsg());
    }

    /**
     * Test trigger with valid job by admin user
     */
    @Test
    public void testTriggerByAdminUser() {
        // Insert a job
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Job to Trigger");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        xxlJobInfoDao.save(jobInfo);

        ReturnT<String> result = xxlJobService.trigger(adminUser, jobInfo.getId(), "test param", "");

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        logger.info("testTriggerByAdminUser: triggered successfully");
    }

    /**
     * Test trigger with valid job by normal user with permission
     */
    @Test
    public void testTriggerByNormalUserWithPermission() {
        // Insert a job in group 1 (user has permission)
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc("Job to Trigger");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        xxlJobInfoDao.save(jobInfo);

        ReturnT<String> result = xxlJobService.trigger(normalUser, jobInfo.getId(), "", "");

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        logger.info("testTriggerByNormalUserWithPermission: triggered successfully");
    }

    /**
     * Test trigger by normal user without permission → FAIL
     */
    @Test
    public void testTriggerByNormalUserWithoutPermission() {
        // Insert a job in group 2 (user only has permission for group 1)
        XxlJobInfo jobInfo = new XxlJobInfo();
        jobInfo.setJobGroup(2);
        jobInfo.setJobDesc("Job to Trigger");
        jobInfo.setAuthor("tester");
        jobInfo.setScheduleType("NONE");
        jobInfo.setMisfireStrategy("DO_NOTHING");
        jobInfo.setExecutorRouteStrategy("FIRST");
        jobInfo.setExecutorHandler("demoJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setGlueType("BEAN");
        xxlJobInfoDao.save(jobInfo);

        ReturnT<String> result = xxlJobService.trigger(normalUser, jobInfo.getId(), "", "");

        assertEquals(ReturnT.FAIL_CODE, result.getCode());
        logger.info("testTriggerByNormalUserWithoutPermission: msg={}", result.getMsg());
    }

    // ---------------------- dashboardInfo ----------------------

    /**
     * Test dashboardInfo with no data
     */
    @Test
    public void testDashboardInfoNoData() {
        Map<String, Object> result = xxlJobService.dashboardInfo();

        assertNotNull(result);
        assertTrue(result.containsKey("jobInfoCount"));
        assertTrue(result.containsKey("jobLogCount"));
        assertTrue(result.containsKey("jobLogSuccessCount"));
        assertTrue(result.containsKey("executorCount"));
        assertEquals(0L, ((Number) result.get("jobInfoCount")).longValue());
        logger.info("testDashboardInfoNoData: jobInfoCount={}", result.get("jobInfoCount"));
    }

    /**
     * Test dashboardInfo with jobs and log report
     */
    @Test
    public void testDashboardInfoWithData() {
        // Insert jobs
        XxlJobInfo job1 = new XxlJobInfo();
        job1.setJobGroup(1);
        job1.setJobDesc("Job 1");
        job1.setAuthor("tester");
        job1.setScheduleType("NONE");
        job1.setMisfireStrategy("DO_NOTHING");
        job1.setExecutorRouteStrategy("FIRST");
        job1.setExecutorHandler("handler1");
        job1.setExecutorBlockStrategy("SERIAL_EXECUTION");
        job1.setGlueType("BEAN");
        xxlJobInfoDao.save(job1);

        XxlJobInfo job2 = new XxlJobInfo();
        job2.setJobGroup(2);
        job2.setJobDesc("Job 2");
        job2.setAuthor("tester");
        job2.setScheduleType("NONE");
        job2.setMisfireStrategy("DO_NOTHING");
        job2.setExecutorRouteStrategy("FIRST");
        job2.setExecutorHandler("handler2");
        job2.setExecutorBlockStrategy("SERIAL_EXECUTION");
        job2.setGlueType("BEAN");
        xxlJobInfoDao.save(job2);

        // Insert log report
        XxlJobLogReport logReport = new XxlJobLogReport();
        logReport.setTriggerDay(new Date());
        logReport.setRunningCount(5);
        logReport.setSucCount(10);
        logReport.setFailCount(2);
        xxlJobLogReportDao.save(logReport);

        Map<String, Object> result = xxlJobService.dashboardInfo();

        assertNotNull(result);
        assertEquals(2L, ((Number) result.get("jobInfoCount")).longValue());
        assertEquals(17L, ((Number) result.get("jobLogCount")).longValue()); // 5+10+2
        assertEquals(10L, ((Number) result.get("jobLogSuccessCount")).longValue());
        logger.info(
                "testDashboardInfoWithData: jobInfoCount={}, jobLogCount={}, successCount={}",
                result.get("jobInfoCount"),
                result.get("jobLogCount"),
                result.get("jobLogSuccessCount"));
    }

    // ---------------------- chartInfo ----------------------

    /**
     * Test chartInfo with no data → returns last 7 days with zeros
     */
    @Test
    public void testChartInfoNoData() {
        Date endDate = new Date();
        Date startDate = new Date(endDate.getTime() - 7L * 24 * 60 * 60 * 1000);

        ReturnT<Map<String, Object>> result = xxlJobService.chartInfo(startDate, endDate);

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertNotNull(result.getContent());
        assertTrue(result.getContent().containsKey("triggerDayList"));
        assertTrue(result.getContent().containsKey("triggerDayCountRunningList"));
        assertTrue(result.getContent().containsKey("triggerDayCountSucList"));
        assertTrue(result.getContent().containsKey("triggerDayCountFailList"));
        logger.info("testChartInfoNoData: returned 7 days of zero data");
    }

    /**
     * Test chartInfo with log report data
     */
    @Test
    public void testChartInfoWithData() {
        // Insert log reports for last 3 days
        for (int i = 0; i < 3; i++) {
            XxlJobLogReport logReport = new XxlJobLogReport();
            logReport.setTriggerDay(new Date(System.currentTimeMillis() - (long) i * 24 * 60 * 60 * 1000));
            logReport.setRunningCount(i + 1);
            logReport.setSucCount((i + 1) * 2);
            logReport.setFailCount(i);
            xxlJobLogReportDao.save(logReport);
        }

        Date endDate = new Date();
        Date startDate = new Date(endDate.getTime() - 7L * 24 * 60 * 60 * 1000);

        ReturnT<Map<String, Object>> result = xxlJobService.chartInfo(startDate, endDate);

        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertNotNull(result.getContent());
        logger.info("testChartInfoWithData: returned chart data with actual values");
    }
}
