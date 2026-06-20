package com.xxl.job.core.thread;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.JobExecutorInitParam;
import com.xxl.job.core.biz.model.JobExecutorParam;
import com.xxl.job.core.biz.model.JobInfoParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.executor.XxlJobExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by xuxueli on 17/3/2.
 */
@Slf4j
public class ExecutorRegistryThread {

    private static final ExecutorRegistryThread INSTANCE = new ExecutorRegistryThread();

    public static ExecutorRegistryThread getInstance() {
        return INSTANCE;
    }

    private Thread registryThread;
    private volatile boolean toStop = false;

    private final List<JobInfoParam> jobInfoParams = new ArrayList<>();

    public void initJobInfoInitParams(List<JobInfoParam> jobInfoParams) {
        this.jobInfoParams.clear();
        this.jobInfoParams.addAll(jobInfoParams);
    }

    /**
     * 执行器注册线程启动
     * @param address 执行器地址
     */
    public void start(String appName, final String address) {

        toStop = false;

        registryThread = new Thread(() -> {
            initJobInfo(appName, XxlJobExecutor.getConfig().getTitle());

            // registry
            while (!toStop) {
                try {
                    RegistryParam registryParam =
                            new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appName, address);
                    for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                        try {
                            ReturnT<String> registryResult = adminBiz.registry(registryParam);
                            if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                                registryResult = ReturnT.SUCCESS;
                                log.debug(
                                        ">>>>>>>>>>> xxl-job registry success, registryParam:{}, registryResult:{}",
                                        registryParam,
                                        registryResult);
                                break;
                            } else {
                                log.info(
                                        ">>>>>>>>>>> xxl-job registry fail, registryParam:{}, registryResult:{}",
                                        registryParam,
                                        registryResult);
                            }
                        } catch (Throwable e) {
                            log.info(">>>>>>>>>>> xxl-job registry error, registryParam:{}", registryParam, e);
                        }
                    }
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }

                try {
                    if (!toStop) {
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    }
                } catch (InterruptedException e) {
                    if (!toStop) {
                        log.warn(
                                ">>>>>>>>>>> xxl-job, executor registry thread interrupted, error msg:{}",
                                e.getMessage());
                    }
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable e) {
                    if (!toStop) {
                        log.warn(">>>>>>>>>>> xxl-job, executor registry thread error, error msg:{}", e.getMessage());
                    }
                }
            }

            // registry remove
            try {
                RegistryParam registryParam =
                        new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appName, address);
                for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                    try {
                        ReturnT<String> registryResult = adminBiz.registryRemove(registryParam);
                        if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                            registryResult = ReturnT.SUCCESS;
                            log.info(
                                    ">>>>>>>>>>> xxl-job registry-remove success, registryParam:{}, registryResult:{}",
                                    registryParam,
                                    registryResult);
                            break;
                        } else {
                            log.info(
                                    ">>>>>>>>>>> xxl-job registry-remove fail, registryParam:{}, registryResult:{}",
                                    registryParam,
                                    registryResult);
                        }
                    } catch (Throwable e) {
                        if (!toStop) {
                            log.info(">>>>>>>>>>> xxl-job registry-remove error, registryParam:{}", registryParam, e);
                        }
                    }
                }
            } catch (Throwable e) {
                if (!toStop) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info(">>>>>>>>>>> xxl-job, executor registry thread destroy.");
        });
        registryThread.setDaemon(true);
        registryThread.setName("xxl-job, executor ExecutorRegistryThread");
        registryThread.start();
    }

    private void initJobInfo(String appName, String title) {

        if (!jobInfoParams.isEmpty()) {
            JobExecutorInitParam initParam = new JobExecutorInitParam();
            initParam.setJobExecutorParam(new JobExecutorParam(appName, title));
            initParam.setJobInfoParamList(jobInfoParams);

            for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                try {
                    ReturnT<String> registryResult = adminBiz.initJobInfo(initParam);
                    if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                        log.debug(
                                ">>>>>>>>>>> xxl-job init success, jobInfoInitParams:{}, jobInfoInitParams:{}",
                                jobInfoParams,
                                registryResult);
                        break;
                    } else {
                        log.info(
                                ">>>>>>>>>>> xxl-job 初始化自动注册失败，请确认xxl-job-admin服务端版本是否支持。 registryResult:{}",
                                registryResult);
                    }
                } catch (Throwable e) {
                    log.warn(">>>>>>>>>>> xxl-job 初始化自动注册失败，请确认xxl-job-admin服务端是否运行正常。 ", e);
                }
            }
        }
    }

    public void toStop() {
        toStop = true;

        // interrupt and wait
        if (registryThread != null) {
            registryThread.interrupt();
            try {
                registryThread.join();
            } catch (InterruptedException e) {
                if (!toStop) {
                    log.warn("xxl-job toStop error, error msg:{}", e.getMessage());
                }
                Thread.currentThread().interrupt();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
