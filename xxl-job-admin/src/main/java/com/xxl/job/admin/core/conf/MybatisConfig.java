package com.xxl.job.admin.core.conf;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis configuration to support multiple databases via VendorDatabaseIdProvider.
 */
@Configuration
public class MybatisConfig {

    @Bean
    @ConditionalOnMissingBean
    public DatabaseIdProvider vendorDatabaseIdProvider() {
        return new VendorDatabaseIdProvider();
    }
}
