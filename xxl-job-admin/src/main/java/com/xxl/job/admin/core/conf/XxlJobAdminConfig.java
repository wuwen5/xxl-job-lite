package com.xxl.job.admin.core.conf;

import com.xxl.job.admin.core.alarm.JobAlarmer;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.dao.*;
import jakarta.annotation.Resource;
import java.util.Arrays;
import javax.sql.DataSource;
import lombok.Getter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * xxl-job config
 *
 * @author xuxueli 2017-04-28
 */
@Component
public class XxlJobAdminConfig implements InitializingBean, DisposableBean {

    private static XxlJobAdminConfig adminConfig = null;

    public static XxlJobAdminConfig getAdminConfig() {
        return adminConfig;
    }

    // ---------------------- XxlJobScheduler ----------------------

    private XxlJobScheduler xxlJobScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {
        XxlJobAdminConfig.adminConfig = this;

        xxlJobScheduler = new XxlJobScheduler();
        xxlJobScheduler.init();
    }

    @Override
    public void destroy() throws Exception {
        xxlJobScheduler.destroy();
    }

    // ---------------------- XxlJobScheduler ----------------------

    @Value("${xxl.job.i18n}")
    private String i18n;

    @Getter
    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Getter
    @Value("${xxl.job.timeout}")
    private int timeout;

    @Getter
    @Value("${spring.mail.from}")
    private String emailFrom;

    @Value("${xxl.job.triggerpool.fast.max}")
    private int triggerPoolFastMax;

    @Value("${xxl.job.triggerpool.slow.max}")
    private int triggerPoolSlowMax;

    @Value("${xxl.job.logretentiondays}")
    private int logretentiondays;

    // dao, service

    @Getter
    @Resource
    private XxlJobLogDao xxlJobLogDao;

    @Getter
    @Resource
    private XxlJobInfoDao xxlJobInfoDao;

    @Getter
    @Resource
    private XxlJobRegistryDao xxlJobRegistryDao;

    @Getter
    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Getter
    @Resource
    private XxlJobLogReportDao xxlJobLogReportDao;

    @Getter
    @Resource
    private JavaMailSender mailSender;

    @Getter
    @Resource
    private DataSource dataSource;

    @Getter
    @Resource
    private JobAlarmer jobAlarmer;

    @Resource
    @Getter
    private JdbcTemplate jdbcTemplate;

    @Resource
    @Getter
    private PlatformTransactionManager platformTransactionManager;

    public String getI18n() {
        if (!Arrays.asList("zh_CN", "zh_TC", "en").contains(i18n)) {
            return "zh_CN";
        }
        return i18n;
    }

    public int getTriggerPoolFastMax() {
        if (triggerPoolFastMax < 200) {
            return 200;
        }
        return triggerPoolFastMax;
    }

    public int getTriggerPoolSlowMax() {
        if (triggerPoolSlowMax < 100) {
            return 100;
        }
        return triggerPoolSlowMax;
    }

    public int getLogretentiondays() {
        if (logretentiondays < 7) {
            // Limit greater than or equal to 7, otherwise close
            return -1;
        }
        return logretentiondays;
    }
}
