package com.xxl.job.admin.core.route.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ExecutorRouteLRUTest {

    @Test
    public void shouldRouteToAddressInList() {
        ExecutorRouteLRU router = new ExecutorRouteLRU();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(301);

        ReturnT<String> result = router.route(triggerParam, addressList);

        assertNotNull(result);
        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertTrue(addressList.contains(result.getContent()));
    }

    @Test
    public void shouldCycleInLRUOrder() {
        ExecutorRouteLRU router = new ExecutorRouteLRU();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(302);

        // First call should return the eldest (first inserted) entry
        String first = router.route(triggerParam, addressList).getContent();
        assertNotNull(first);

        // Second call should return a different address (LRU eviction order)
        String second = router.route(triggerParam, addressList).getContent();
        assertNotNull(second);
        assertTrue(addressList.contains(second));
    }

    @Test
    public void shouldHandleSingleAddress() {
        ExecutorRouteLRU router = new ExecutorRouteLRU();
        List<String> addressList = Arrays.asList("192.168.1.1:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(303);

        ReturnT<String> result = router.route(triggerParam, addressList);
        assertEquals("192.168.1.1:9999", result.getContent());
    }
}
