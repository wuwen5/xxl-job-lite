package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.complete.XxlJobCompleter;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.DateUtil;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * job lose-monitor instance
 *
 * @author xuxueli 2015-9-1 18:05:56
 */
@Slf4j
public class JobCompleteHelper {

    private static JobCompleteHelper instance = new JobCompleteHelper();

    public static JobCompleteHelper getInstance() {
        return instance;
    }

    // ---------------------- monitor ----------------------

    private ThreadPoolExecutor callbackThreadPool = null;
    private Thread monitorThread;
    private volatile boolean toStop = false;

    public void start() {

        // for callback
        callbackThreadPool = new ThreadPoolExecutor(
                2,
                20,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(3000),
                r -> new Thread(r, "xxl-job, admin JobLosedMonitorHelper-callbackThreadPool-" + r.hashCode()),
                (r, executor) -> {
                    r.run();
                    log.warn(">>>>>>>>>>> xxl-job, callback too fast, match threadpool rejected handler(run now).");
                });

        // for monitor
        monitorThread = new Thread(
                () -> {

                    // wait for JobTriggerPoolHelper-init
                    sleep(TimeUnit.MILLISECONDS, 50);

                    // monitor
                    while (!toStop) {
                        try {
                            // 任务结果丢失处理：调度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线，则将本地调度主动标记失败；
                            Date losedTime = DateUtil.addMinutes(new Date(), -10);
                            List<Long> losedJobIds = XxlJobAdminConfig.getAdminConfig()
                                    .getXxlJobLogDao()
                                    .findLostJobIds(losedTime);

                            if (losedJobIds != null) {
                                losedJobIds.forEach(logId -> XxlJobCompleter.updateHandleInfoAndFinish(new XxlJobLog()
                                        .setId(logId)
                                        .setHandleTime(new Date())
                                        .setHandleCode(ReturnT.FAIL_CODE)
                                        .setHandleMsg(I18nUtil.getString("joblog_lost_fail"))));
                            }
                        } catch (Throwable e) {
                            if (!toStop) {
                                log.error(">>>>>>>>>>> xxl-job, job fail monitor thread error.", e);
                            }
                        }

                        sleep(TimeUnit.SECONDS, 60);
                    }

                    log.info(">>>>>>>>>>> xxl-job, JobLosedMonitorHelper stop");
                },
                "xxl-job, admin JobLosedMonitorHelper");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void sleep(TimeUnit timeUnit, int timeout) {
        try {
            timeUnit.sleep(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (!toStop) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void toStop() {
        toStop = true;

        // stop registryOrRemoveThreadPool
        callbackThreadPool.shutdownNow();

        // stop monitorThread (interrupt and wait)
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error(e.getMessage(), e);
        }
    }

    // ---------------------- helper ----------------------

    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {

        callbackThreadPool.execute(() -> {
            for (HandleCallbackParam handleCallbackParam : callbackParamList) {
                ReturnT<String> callbackResult = callback(handleCallbackParam);
                log.debug(
                        ">>>>>>>>> JobApiController.callback {}, handleCallbackParam={}, callbackResult={}",
                        (callbackResult.getCode() == ReturnT.SUCCESS_CODE ? "success" : "fail"),
                        handleCallbackParam,
                        callbackResult);
            }
        });

        return ReturnT.SUCCESS;
    }

    private ReturnT<String> callback(HandleCallbackParam handleCallbackParam) {
        // valid log item
        XxlJobLog log = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().load(handleCallbackParam.getLogId());
        if (log == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "log item not found.");
        }
        if (log.getHandleCode() > 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "log repeate callback.");
        }

        // handle msg
        StringBuilder handleMsg = new StringBuilder();
        if (log.getHandleMsg() != null) {
            handleMsg.append(log.getHandleMsg()).append("<br>");
        }
        if (handleCallbackParam.getHandleMsg() != null) {
            handleMsg.append(handleCallbackParam.getHandleMsg());
        }

        // success, save log
        log.setHandleTime(new Date());
        log.setHandleCode(handleCallbackParam.getHandleCode());
        log.setHandleMsg(handleMsg.toString());
        XxlJobCompleter.updateHandleInfoAndFinish(log);

        return ReturnT.SUCCESS;
    }
}
