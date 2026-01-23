package com.xxl.job.core.executorbiz;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wuwen
 */
@Configuration
public class TestConfiguration {
    
    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor() {

        final XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses("http://127.0.0.1:18760");
        executor.setAppname("xxljob-ut");
        return executor;
    }
    
    @Bean
    public ExampleJob exampleJob() {
        return new ExampleJob();
    }
}
