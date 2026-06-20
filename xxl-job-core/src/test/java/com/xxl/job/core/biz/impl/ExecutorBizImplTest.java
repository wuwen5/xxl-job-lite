package com.xxl.job.core.biz.impl;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.handler.IJobHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ExecutorBizImpl} focusing on the GLUE-enabled toggle logic.
 */
class ExecutorBizImplTest {

    private ExecutorBizImpl executorBizImpl;

    static XxlJobExecutor executor = new XxlJobExecutor();

    @BeforeEach
    void setUp() {
        executorBizImpl = new ExecutorBizImpl();
    }

    private static TriggerParam buildTriggerParam(GlueTypeEnum glueType) {
        TriggerParam param = new TriggerParam();
        param.setJobId(99);
        param.setGlueType(glueType.name());
        param.setGlueSource("echo hello");
        param.setGlueUpdatetime(System.currentTimeMillis());
        param.setExecutorBlockStrategy(ExecutorBlockStrategyEnum.SERIAL_EXECUTION.name());
        param.setLogId(1);
        param.setLogDateTime(System.currentTimeMillis());
        return param;
    }

    // ---------------------- tests ----------------------

    /**
     * When glueEnabled=false and a GLUE_GROOVY job is dispatched, the executor should
     * reject it immediately with FAIL_CODE.
     */
    @Test
    void run_glueGroovy_whenGlueDisabled_shouldReturnFail() {
        executor.setGlueEnabled(false);

        TriggerParam param = buildTriggerParam(GlueTypeEnum.GLUE_GROOVY);
        ReturnT<String> result = executorBizImpl.run(param);

        Assertions.assertEquals(ReturnT.FAIL_CODE, result.getCode());
        Assertions.assertTrue(
                result.getMsg().contains("GLUE(Java)"), "Error message should contain the glue type desc");
    }

    /**
     * When glueEnabled=false and a script (Shell) job is dispatched, the executor should
     * reject it immediately with FAIL_CODE.
     */
    @Test
    void run_glueShell_whenGlueDisabled_shouldReturnFail() throws Exception {
        executor.setGlueEnabled(false);

        TriggerParam param = buildTriggerParam(GlueTypeEnum.GLUE_SHELL);
        ReturnT<String> result = executorBizImpl.run(param);

        Assertions.assertEquals(ReturnT.FAIL_CODE, result.getCode());
        Assertions.assertTrue(
                result.getMsg().contains("GLUE(Shell)"), "Error message should contain the glue type desc");
    }

    /**
     * When glueEnabled=false, BEAN-type jobs must still be accepted (the toggle only
     * affects non-BEAN types).
     */
    @Test
    void run_bean_whenGlueDisabled_shouldNotRejectOnGlueCheck() throws Exception {
        executor.setGlueEnabled(false);

        // Register a handler so the BEAN path can proceed past the handler-lookup check
        XxlJobExecutor.registJobHandler("glueDisabledBeanHandler", new IJobHandler() {
            @Override
            public void execute() {}
        });

        TriggerParam param = new TriggerParam();
        param.setJobId(100);
        param.setExecutorHandler("glueDisabledBeanHandler");
        param.setGlueType(GlueTypeEnum.BEAN.name());
        param.setExecutorBlockStrategy(ExecutorBlockStrategyEnum.SERIAL_EXECUTION.name());
        param.setLogId(2);
        param.setLogDateTime(System.currentTimeMillis());

        ReturnT<String> result = executorBizImpl.run(param);

        // The GLUE-guard must not reject the request; handler was found so it succeeds
        Assertions.assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
    }

    /**
     * When glueEnabled=true (default), a GLUE_GROOVY job is not rejected by the guard
     * and proceeds to the next validation stage.
     */
    @Test
    void run_glueGroovy_whenGlueEnabled_shouldPassGuard() {
        executor.setGlueEnabled(true);

        TriggerParam param = buildTriggerParam(GlueTypeEnum.GLUE_GROOVY);
        ReturnT<String> result = executorBizImpl.run(param);

        // The guard passes; the GROOVY path will fail on loading null/invalid source,
        // but the rejection must NOT carry the "not supported" guard message.
        Assertions.assertFalse(
                result.getMsg() != null && result.getMsg().contains("not supported"),
                "Guard must not reject when glueEnabled=true");
    }
}
