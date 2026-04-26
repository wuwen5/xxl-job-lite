package com.xxl.job.admin.util;

import com.xxl.job.admin.AbstractTest;
import com.xxl.job.admin.core.util.I18nUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * email util test
 *
 * @author xuxueli 2017-12-22 17:16:23
 */
public class I18nUtilTest extends AbstractTest {
    private static Logger logger = LoggerFactory.getLogger(I18nUtilTest.class);

    @Test
    public void test() {
        logger.info(I18nUtil.getString("admin_name"));
        logger.info(I18nUtil.getMultString("admin_name", "admin_name_full"));
        logger.info(I18nUtil.getMultString());
    }
}
