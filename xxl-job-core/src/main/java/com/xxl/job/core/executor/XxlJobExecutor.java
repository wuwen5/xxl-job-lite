package com.xxl.job.core.executor;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.impl.MethodJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.server.EmbedServer;
import com.xxl.job.core.thread.JobLogFileCleanThread;
import com.xxl.job.core.thread.JobThread;
import com.xxl.job.core.thread.TriggerCallbackThread;
import com.xxl.job.core.util.IpUtil;
import com.xxl.job.core.util.NetUtil;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by xuxueli on 2016/3/2 21:14.
 */
@Slf4j
public class XxlJobExecutor {

    /**
     * executor config
     */
    private static final AtomicReference<XxlJobExecutorConfig> CONFIG_REF = new AtomicReference<>(
            XxlJobExecutorConfig.builder().glueEnabled(true).build());

    /**
     * admin registry address splitter
     */
    private static final String ADDRESS_SPLITTER = ",";

    /**
     * default executor port
     */
    private static final int DEFAULT_PORT = 9999;

    /**
     * ---------------------- admin-client (rpc invoker) ----------------------
     */
    private static final List<AdminBiz> ADMIN_BIZ_LIST = new ArrayList<>();

    public static XxlJobExecutorConfig getConfig() {
        return CONFIG_REF.get();
    }

    public void start() throws Exception {

        // init logpath
        XxlJobFileAppender.initLogPath(XxlJobExecutor.getConfig().getLogPath());

        // init invoker, admin-client
        initAdminBizList(
                XxlJobExecutor.getConfig().getAdminAddresses(),
                XxlJobExecutor.getConfig().getAccessToken(),
                XxlJobExecutor.getConfig().getTimeout());

        // init JobLogFileCleanThread
        JobLogFileCleanThread.getInstance().start(XxlJobExecutor.getConfig().getLogRetentionDays());

        // init TriggerCallbackThread
        TriggerCallbackThread.getInstance().start();

        // init executor-server
        initEmbedServer();
    }

    public void destroy() {
        // destroy executor-server
        stopEmbedServer();

        // destroy jobThreadRepository
        if (!JOB_THREAD_REPOSITORY.isEmpty()) {
            for (Map.Entry<Integer, JobThread> item : JOB_THREAD_REPOSITORY.entrySet()) {
                JobThread oldJobThread = removeJobThread(item.getKey(), "web container destroy and kill the job.");
                // wait for job thread push result to callback queue
                if (oldJobThread != null) {
                    try {
                        oldJobThread.join();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error(">>>>>>>>>>> xxl-job, JobThread destroy(join) error, jobId:{}", item.getKey(), e);
                    }
                }
            }
            JOB_THREAD_REPOSITORY.clear();
        }
        JOB_HANDLER_REPOSITORY.clear();

        // destroy JobLogFileCleanThread
        JobLogFileCleanThread.getInstance().toStop();

        // destroy TriggerCallbackThread
        TriggerCallbackThread.getInstance().toStop();
    }

    public void setAdminAddresses(String adminAddresses) {
        updateConfig(config -> config.withAdminAddresses(adminAddresses));
    }

    public void setAccessToken(String accessToken) {
        updateConfig(config -> config.withAccessToken(accessToken));
    }

    public void setTimeout(int timeout) {
        updateConfig(config -> config.withTimeout(timeout));
    }

    public void setAppname(String appname) {
        updateConfig(config -> config.withAppname(appname));
    }

    public void setTitle(String title) {
        updateConfig(config -> config.withTitle(title));
    }

    public void setAddress(String address) {
        updateConfig(config -> config.withAddress(address));
    }

    public void setIp(String ip) {
        updateConfig(config -> config.withIp(ip));
    }

    public void setPort(int port) {
        updateConfig(config -> config.withPort(port));
    }

    public void setLogPath(String logPath) {
        updateConfig(config -> config.withLogPath(logPath));
    }

    public void setLogRetentionDays(int logRetentionDays) {
        updateConfig(config -> config.withLogRetentionDays(logRetentionDays));
    }

    public void setGlueEnabled(boolean glueEnabled) {
        updateConfig(config -> config.withGlueEnabled(glueEnabled));
    }

    private void updateConfig(Function<XxlJobExecutorConfig, XxlJobExecutorConfig> updater) {
        XxlJobExecutorConfig current = CONFIG_REF.get();
        XxlJobExecutorConfig newConf = updater.apply(current);
        // 暂无并发更新场景，暂不使用CAS方式更新配置
        CONFIG_REF.set(newConf);
    }

    private void initAdminBizList(String adminAddresses, String accessToken, int timeout) {
        if (adminAddresses != null && !adminAddresses.trim().isEmpty()) {
            ADMIN_BIZ_LIST.clear();
            for (String address : adminAddresses.trim().split(ADDRESS_SPLITTER)) {
                String trim = address.trim();
                if (!trim.isEmpty()) {
                    AdminBiz adminBiz = new AdminBizClient(trim, accessToken, timeout);
                    ADMIN_BIZ_LIST.add(adminBiz);
                }
            }
        }
    }

    public static List<AdminBiz> getAdminBizList() {
        return Collections.unmodifiableList(ADMIN_BIZ_LIST);
    }

    /**
     * ---------------------- executor-server (rpc provider) ----------------------
     */
    private EmbedServer embedServer = null;

    private void initEmbedServer() {

        // fill ip port
        int port = XxlJobExecutor.getConfig().getPort();
        String ip = XxlJobExecutor.getConfig().getIp();
        String accessToken = XxlJobExecutor.getConfig().getAccessToken();
        String address = XxlJobExecutor.getConfig().getAddress();

        port = port > 0 ? port : NetUtil.findAvailablePort(DEFAULT_PORT);
        ip = (ip != null && !ip.trim().isEmpty()) ? ip : IpUtil.getIp();

        // generate address
        if (address == null || address.trim().isEmpty()) {
            // registry-address：default use address to registry , otherwise use ip:port if address is null
            String ipPortAddress = IpUtil.getIpPort(ip, port);
            address = "http://{ip_port}/".replace("{ip_port}", ipPortAddress);
        }

        // accessToken
        if (accessToken == null || accessToken.trim().isEmpty()) {
            log.warn(
                    ">>>>>>>>>>> xxl-job accessToken is empty. To ensure system security, please set the accessToken.");
        }

        // start
        embedServer = new EmbedServer();
        embedServer.start(address, port, accessToken);
    }

    private void stopEmbedServer() {
        // stop provider factory
        if (embedServer != null) {
            try {
                embedServer.stop();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * ---------------------- job handler repository ----------------------
     */
    private static final ConcurrentMap<String, IJobHandler> JOB_HANDLER_REPOSITORY = new ConcurrentHashMap<>();

    public static IJobHandler loadJobHandler(String name) {
        return JOB_HANDLER_REPOSITORY.get(name);
    }

    public static IJobHandler registJobHandler(String name, IJobHandler jobHandler) {
        log.info(">>>>>>>>>>> xxl-job register jobhandler success, name:{}, jobHandler:{}", name, jobHandler);
        return JOB_HANDLER_REPOSITORY.put(name, jobHandler);
    }

    protected void registJobHandler(XxlJob xxlJob, Object bean, Method executeMethod) {
        if (xxlJob == null) {
            return;
        }

        String name = xxlJob.value();
        // make and simplify the variables since they'll be called several times later
        Class<?> clazz = bean.getClass();
        String methodName = executeMethod.getName();
        if (name.trim().isEmpty()) {
            throw new RuntimeException(
                    "xxl-job method-jobhandler name invalid, for[" + clazz + "#" + methodName + "] .");
        }
        if (loadJobHandler(name) != null) {
            throw new RuntimeException("xxl-job jobhandler[" + name + "] naming conflicts.");
        }

        executeMethod.setAccessible(true);

        // init and destroy
        Method initMethod = null;
        Method destroyMethod = null;

        if (!xxlJob.init().trim().isEmpty()) {
            try {
                initMethod = clazz.getDeclaredMethod(xxlJob.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                        "xxl-job method-jobhandler initMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }
        if (!xxlJob.destroy().trim().isEmpty()) {
            try {
                destroyMethod = clazz.getDeclaredMethod(xxlJob.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                        "xxl-job method-jobhandler destroyMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }

        // registry jobhandler
        registJobHandler(name, new MethodJobHandler(bean, executeMethod, initMethod, destroyMethod));
    }

    /**
     * ---------------------- job thread repository ----------------------
     */
    private static final ConcurrentMap<Integer, JobThread> JOB_THREAD_REPOSITORY = new ConcurrentHashMap<>();

    public static JobThread registJobThread(int jobId, IJobHandler handler, String removeOldReason) {
        JobThread newJobThread = new JobThread(jobId, handler);
        newJobThread.start();
        log.info(">>>>>>>>>>> xxl-job regist JobThread success, jobId:{}, handler:{}", jobId, handler);

        // putIfAbsent | oh my god, map's put method return the old value!!!
        JobThread oldJobThread = JOB_THREAD_REPOSITORY.put(jobId, newJobThread);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }

        return newJobThread;
    }

    public static JobThread removeJobThread(int jobId, String removeOldReason) {
        JobThread oldJobThread = JOB_THREAD_REPOSITORY.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();

            return oldJobThread;
        }
        return null;
    }

    public static JobThread loadJobThread(int jobId) {
        return JOB_THREAD_REPOSITORY.get(jobId);
    }
}
