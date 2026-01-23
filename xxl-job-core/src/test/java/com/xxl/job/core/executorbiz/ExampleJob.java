package com.xxl.job.core.executorbiz;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuwen
 */
@Slf4j
public class ExampleJob {
    
    @XxlJob(value = "testNoInitJobHandler")
    public void execute() {
        log.info("ExampleJob is executing...");
    }

    @XxlJob(value = "testInitJobHandler", cron = "0/10 * * * * ?", desc = "测试自动注册的任务")
    public void autoInitJob() {
        log.info("ExampleJob is executing...");
    }
}
