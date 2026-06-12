package com.xxl.job.admin.core.route.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ExecutorRouteRoundTest {

    @Test
    public void shouldRouteToAddressInList() {
        ExecutorRouteRound router = new ExecutorRouteRound();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(401);

        ReturnT<String> result = router.route(triggerParam, addressList);

        assertNotNull(result);
        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertTrue(addressList.contains(result.getContent()));
    }

    @Test
    public void shouldRoundRobinAcrossAddresses() {
        ExecutorRouteRound router = new ExecutorRouteRound();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(402);

        // Route enough times to hit all addresses
        Set<String> routedAddresses = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            ReturnT<String> result = router.route(triggerParam, addressList);
            routedAddresses.add(result.getContent());
        }

        // Round-robin should eventually hit all addresses
        assertEquals(3, routedAddresses.size());
    }

    @Test
    public void shouldHandleSingleAddress() {
        ExecutorRouteRound router = new ExecutorRouteRound();
        List<String> addressList = Arrays.asList("192.168.1.1:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(403);

        ReturnT<String> result = router.route(triggerParam, addressList);
        assertEquals("192.168.1.1:9999", result.getContent());
    }
}
