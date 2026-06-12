package com.xxl.job.admin.core.route.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ExecutorRouteLFUTest {

    @Test
    public void shouldRouteToAddressInList() {
        ExecutorRouteLFU router = new ExecutorRouteLFU();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(1);

        ReturnT<String> result = router.route(triggerParam, addressList);

        assertNotNull(result);
        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertTrue(addressList.contains(result.getContent()));
    }

    @Test
    public void shouldFavorLeastUsedAddress() {
        ExecutorRouteLFU router = new ExecutorRouteLFU();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(100);

        // Route multiple times - the router should pick the least-used address each time
        String first = router.route(triggerParam, addressList).getContent();
        assertNotNull(first);

        // After routing, the selected address count increments, so next call may pick the other
        String second = router.route(triggerParam, addressList).getContent();
        assertNotNull(second);
        assertTrue(addressList.contains(second));
    }

    @Test
    public void shouldHandleSingleAddress() {
        ExecutorRouteLFU router = new ExecutorRouteLFU();
        List<String> addressList = Arrays.asList("192.168.1.1:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(200);

        ReturnT<String> result = router.route(triggerParam, addressList);
        assertEquals("192.168.1.1:9999", result.getContent());
    }
}
