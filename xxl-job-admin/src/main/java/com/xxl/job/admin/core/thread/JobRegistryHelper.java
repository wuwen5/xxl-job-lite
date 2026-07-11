package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * job registry instance
 * @author xuxueli 2016-10-02 19:10:24
 */
@Slf4j
public class JobRegistryHelper {

    private static JobRegistryHelper instance = new JobRegistryHelper();

    public static JobRegistryHelper getInstance() {
        return instance;
    }

    private ThreadPoolExecutor registryOrRemoveThreadPool = null;
    private Thread registryMonitorThread;
    private volatile boolean toStop = false;

    public void start() {

        // for registry or remove
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
                2,
                10,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> new Thread(
                        r, "xxl-job, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode()));

        // for monitor
        registryMonitorThread = new Thread(() -> {
            while (!toStop) {
                try {
                    // auto registry group
                    List<XxlJobGroup> groupList = XxlJobAdminConfig.getAdminConfig()
                            .getXxlJobGroupDao()
                            .findByAddressType(0);
                    if (groupList != null && !groupList.isEmpty()) {

                        // remove dead address (admin/executor)
                        List<Integer> ids = XxlJobAdminConfig.getAdminConfig()
                                .getXxlJobRegistryDao()
                                .findDead(RegistryConfig.DEAD_TIMEOUT, new Date());
                        if (ids != null && !ids.isEmpty()) {
                            XxlJobAdminConfig.getAdminConfig()
                                    .getXxlJobRegistryDao()
                                    .removeDead(ids);
                        }

                        // fresh online address (admin/executor)
                        List<XxlJobRegistry> list = XxlJobAdminConfig.getAdminConfig()
                                .getXxlJobRegistryDao()
                                .findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
                        Map<String, Set<String>> appAddressMap = new HashMap<>();
                        if (list != null) {
                            for (XxlJobRegistry item : list) {
                                if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                                    appAddressMap
                                            .computeIfAbsent(item.getRegistryKey(), k -> new LinkedHashSet<>())
                                            .add(item.getRegistryValue());
                                }
                            }
                        }

                        // fresh group address
                        for (XxlJobGroup group : groupList) {
                            Set<String> registrySet = appAddressMap.get(group.getAppname());

                            String addressListStr = null;
                            if (registrySet != null && !registrySet.isEmpty()) {
                                List<String> registryList = new ArrayList<>(registrySet);
                                Collections.sort(registryList);
                                addressListStr = String.join(",", registryList);
                            }

                            group.setAddressList(addressListStr);
                            group.setUpdateTime(new Date());

                            XxlJobAdminConfig.getAdminConfig()
                                    .getXxlJobGroupDao()
                                    .update(group);
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> xxl-job, job registry monitor thread error.", e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (!toStop) {
                        log.error(">>>>>>>>>>> xxl-job, job registry monitor thread error.", e);
                    }
                }
            }
            log.info(">>>>>>>>>>> xxl-job, job registry monitor thread stop");
        });
        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("xxl-job, admin JobRegistryMonitorHelper-registryMonitorThread");
        registryMonitorThread.start();
    }

    public void toStop() {
        toStop = true;

        // stop registryOrRemoveThreadPool
        registryOrRemoveThreadPool.shutdownNow();

        // stop monitir (interrupt and wait)
        registryMonitorThread.interrupt();
        try {
            registryMonitorThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(e.getMessage(), e);
        }
    }

    // ---------------------- helper ----------------------

    public ReturnT<String> registry(RegistryParam registryParam) {

        // valid
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        // async execute
        registryOrRemoveThreadPool.execute(() -> {
            int ret = XxlJobAdminConfig.getAdminConfig()
                    .getXxlJobRegistryDao()
                    .registryUpdate(
                            registryParam.getRegistryGroup(),
                            registryParam.getRegistryKey(),
                            registryParam.getRegistryValue(),
                            new Date());
            if (ret < 1) {
                XxlJobAdminConfig.getAdminConfig()
                        .getXxlJobRegistryDao()
                        .registrySave(
                                registryParam.getRegistryGroup(),
                                registryParam.getRegistryKey(),
                                registryParam.getRegistryValue(),
                                new Date());

                // fresh
                freshGroupRegistryInfo(registryParam);
            }
        });

        return ReturnT.SUCCESS;
    }

    public ReturnT<String> registryRemove(RegistryParam registryParam) {

        // valid
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        // async execute
        registryOrRemoveThreadPool.execute(() -> {
            int ret = XxlJobAdminConfig.getAdminConfig()
                    .getXxlJobRegistryDao()
                    .registryDelete(
                            registryParam.getRegistryGroup(),
                            registryParam.getRegistryKey(),
                            registryParam.getRegistryValue());
            if (ret > 0) {
                // fresh
                freshGroupRegistryInfo(registryParam);
            }
        });

        return ReturnT.SUCCESS;
    }

    private void freshGroupRegistryInfo(RegistryParam registryParam) {
        // Under consideration, prevent affecting core tables
    }
}
