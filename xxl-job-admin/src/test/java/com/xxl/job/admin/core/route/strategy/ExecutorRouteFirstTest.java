package com.xxl.job.admin.core.route.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ExecutorRouteFirst
 */
public class ExecutorRouteFirstTest {

    @Test
    public void shouldRouteToFirstAddress() {
        ExecutorRouteFirst router = new ExecutorRouteFirst();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(1);

        ReturnT<String> result = router.route(triggerParam, addressList);

        assertNotNull(result);
        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertEquals("192.168.1.1:9999", result.getContent());
    }

    @Test
    public void shouldAlwaysReturnFirstAddress() {
        ExecutorRouteFirst router = new ExecutorRouteFirst();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(2);

        // Route multiple times - should always return first address
        for (int i = 0; i < 10; i++) {
            ReturnT<String> result = router.route(triggerParam, addressList);
            assertEquals("192.168.1.1:9999", result.getContent());
        }
    }

    @Test
    public void shouldHandleSingleAddress() {
        ExecutorRouteFirst router = new ExecutorRouteFirst();
        List<String> addressList = Collections.singletonList("192.168.1.1:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(3);

        ReturnT<String> result = router.route(triggerParam, addressList);

        assertNotNull(result);
        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertEquals("192.168.1.1:9999", result.getContent());
    }

    @Test
    public void shouldHandleTwoAddresses() {
        ExecutorRouteFirst router = new ExecutorRouteFirst();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(4);

        ReturnT<String> result = router.route(triggerParam, addressList);

        assertNotNull(result);
        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertEquals("192.168.1.1:9999", result.getContent());
    }
}
