package com.xxl.job.core.thread;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.util.FileUtil;
import com.xxl.job.core.util.JdkSerializeTool;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by xuxueli on 16/7/22.
 */
@Slf4j
public class TriggerCallbackThread {

    private static final TriggerCallbackThread INSTANCE = new TriggerCallbackThread();

    public static TriggerCallbackThread getInstance() {
        return INSTANCE;
    }

    /**
     * job results callback queue
     */
    private final LinkedBlockingQueue<HandleCallbackParam> callBackQueue = new LinkedBlockingQueue<>();

    public static void pushCallBack(HandleCallbackParam callback) {
        getInstance().callBackQueue.add(callback);
        log.debug(">>>>>>>>>>> xxl-job, push callback request, logId:{}", callback.getLogId());
    }

    /**
     * callback thread
     */
    private Thread triggerCallbackThread;

    private Thread triggerRetryCallbackThread;
    private volatile boolean toStop = false;

    public void start() {

        toStop = false;
        // callback
        triggerCallbackThread = new Thread(() -> {

            // normal callback
            while (!toStop) {
                try {
                    HandleCallbackParam callback = getInstance().callBackQueue.take();
                    // callback list param
                    List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                    getInstance().callBackQueue.drainTo(callbackParamList);
                    callbackParamList.add(callback);

                    // callback, will retry if error
                    doCallback(callbackParamList);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        log.warn("xxl-job, executor callback thread interrupted, errorMsg:{}", e.getMessage());
                    }
                    Thread.currentThread().interrupt();
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }
            }

            // last callback
            try {
                List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                getInstance().callBackQueue.drainTo(callbackParamList);
                if (!callbackParamList.isEmpty()) {
                    doCallback(callbackParamList);
                }
            } catch (Throwable e) {
                if (!toStop) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info(">>>>>>>>>>> xxl-job, executor callback thread destroy.");
        });
        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.setName("xxl-job, executor TriggerCallbackThread");
        triggerCallbackThread.start();

        // retry
        triggerRetryCallbackThread = new Thread(() -> {
            while (!toStop) {
                try {
                    retryFailCallbackFile();
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (InterruptedException ie) {
                    if (!toStop) {
                        log.warn("xxl-job, executor retry callback thread interrupted, errorMsg:{}", ie.getMessage());
                    }
                    Thread.currentThread().interrupt();
                }
            }
            log.info(">>>>>>>>>>> xxl-job, executor retry callback thread destroy.");
        });
        triggerRetryCallbackThread.setDaemon(true);
        triggerRetryCallbackThread.start();
    }

    public void toStop() {
        toStop = true;
        // stop callback, interrupt and wait
        if (triggerCallbackThread != null) {
            triggerCallbackThread.interrupt();
            try {
                triggerCallbackThread.join();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error(ie.getMessage(), ie);
            }
        }

        // stop retry, interrupt and wait
        if (triggerRetryCallbackThread != null) {
            triggerRetryCallbackThread.interrupt();
            try {
                triggerRetryCallbackThread.join();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error(ie.getMessage(), ie);
            }
        }
    }

    /**
     * do callback, will retry if error
     */
    private void doCallback(List<HandleCallbackParam> callbackParamList) {
        boolean callbackRet = false;
        // callback, will retry if error
        for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
            try {
                ReturnT<String> callbackResult = adminBiz.callback(callbackParamList);
                if (callbackResult != null && ReturnT.SUCCESS_CODE == callbackResult.getCode()) {
                    callbackLog(callbackParamList, "<br>----------- xxl-job job callback finish.");
                    callbackRet = true;
                    break;
                } else {
                    callbackLog(
                            callbackParamList,
                            "<br>----------- xxl-job job callback fail, callbackResult:" + callbackResult);
                }
            } catch (Exception e) {
                callbackLog(
                        callbackParamList, "<br>----------- xxl-job job callback error, errorMsg:" + e.getMessage());
            }
        }
        if (!callbackRet) {
            appendFailCallbackFile(callbackParamList);
        }
    }

    /**
     * callback log
     */
    private void callbackLog(List<HandleCallbackParam> callbackParamList, String logContent) {
        for (HandleCallbackParam callbackParam : callbackParamList) {
            String logFileName = XxlJobFileAppender.makeLogFileName(
                    new Date(callbackParam.getLogDateTim()), callbackParam.getLogId());
            XxlJobContext.setXxlJobContext(new XxlJobContext(-1, null, logFileName, -1, -1));
            XxlJobHelper.log(logContent);
        }
    }

    // ---------------------- fail-callback file ----------------------

    private static final String FAIL_CALLBACK_FILE_PATH = XxlJobFileAppender.getLogPath()
            .concat(File.separator)
            .concat("callbacklog")
            .concat(File.separator);
    private static final String FAIL_CALLBACK_FILE_NAME =
            FAIL_CALLBACK_FILE_PATH.concat("xxl-job-callback-{x}").concat(".log");

    private void appendFailCallbackFile(List<HandleCallbackParam> callbackParamList) {
        // valid
        if (callbackParamList == null || callbackParamList.isEmpty()) {
            return;
        }

        // append file
        byte[] callbackParamListBytes = JdkSerializeTool.serialize(callbackParamList);

        File callbackLogFile =
                new File(FAIL_CALLBACK_FILE_NAME.replace("{x}", String.valueOf(System.currentTimeMillis())));
        if (callbackLogFile.exists()) {
            for (int i = 0; i < 100; i++) {
                callbackLogFile = new File(FAIL_CALLBACK_FILE_NAME.replace(
                        "{x}",
                        String.valueOf(System.currentTimeMillis()).concat("-").concat(String.valueOf(i))));
                if (!callbackLogFile.exists()) {
                    break;
                }
            }
        }
        FileUtil.writeFileContent(callbackLogFile, callbackParamListBytes);
    }

    private void retryFailCallbackFile() {

        // valid
        File callbackLogPath = new File(FAIL_CALLBACK_FILE_PATH);
        if (!callbackLogPath.exists()) {
            return;
        }
        if (callbackLogPath.isFile() && !callbackLogPath.delete()) {
            log.warn("Failed to delete invalid callback log file: {}", callbackLogPath);
        }
        if (!(callbackLogPath.isDirectory())) {
            return;
        }
        File[] callbackLogFiles = callbackLogPath.listFiles();
        if (callbackLogFiles == null) {
            return;
        }

        // load and clear file, retry
        for (File callbaclLogFile : callbackLogFiles) {
            byte[] callbackParamListBytes = FileUtil.readFileContent(callbaclLogFile);

            // avoid empty file
            if (callbackParamListBytes == null || callbackParamListBytes.length < 1) {
                if (!callbaclLogFile.delete()) {
                    log.warn("Failed to delete empty callback log file: {}", callbaclLogFile);
                }
                continue;
            }

            List<HandleCallbackParam> callbackParamList =
                    (List<HandleCallbackParam>) JdkSerializeTool.deserialize(callbackParamListBytes, List.class);

            if (!callbaclLogFile.delete()) {
                log.warn("Failed to delete callback log file after reading: {}", callbaclLogFile);
            }
            doCallback(callbackParamList);
        }
    }
}
