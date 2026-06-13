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

/**
 * Unit tests for ExecutorRouteRandom
 */
public class ExecutorRouteRandomTest {

    @Test
    public void shouldRouteToAddressInList() {
        ExecutorRouteRandom router = new ExecutorRouteRandom();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(1);

        ReturnT<String> result = router.route(triggerParam, addressList);

        assertNotNull(result);
        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertTrue(addressList.contains(result.getContent()));
    }

    @Test
    public void shouldDistributeAcrossAddresses() {
        ExecutorRouteRandom router = new ExecutorRouteRandom();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(2);

        // Route many times to verify randomness distributes across addresses
        Set<String> routedAddresses = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ReturnT<String> result = router.route(triggerParam, addressList);
            routedAddresses.add(result.getContent());
        }

        // With 100 random routes across 3 addresses, we should hit all of them
        assertEquals(3, routedAddresses.size());
    }

    @Test
    public void shouldHandleSingleAddress() {
        ExecutorRouteRandom router = new ExecutorRouteRandom();
        List<String> addressList = List.of("192.168.1.1:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(3);

        // Route multiple times with single address
        for (int i = 0; i < 10; i++) {
            ReturnT<String> result = router.route(triggerParam, addressList);
            assertEquals("192.168.1.1:9999", result.getContent());
        }
    }

    @Test
    public void shouldHandleTwoAddresses() {
        ExecutorRouteRandom router = new ExecutorRouteRandom();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(4);

        ReturnT<String> result = router.route(triggerParam, addressList);

        assertNotNull(result);
        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertTrue(addressList.contains(result.getContent()));
    }

    @Test
    public void shouldReturnDifferentAddressesOverMultipleCalls() {
        ExecutorRouteRandom router = new ExecutorRouteRandom();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(5);

        // Track if we get different addresses
        String firstAddress = null;
        boolean foundDifferent = false;

        for (int i = 0; i < 20; i++) {
            ReturnT<String> result = router.route(triggerParam, addressList);
            if (firstAddress == null) {
                firstAddress = result.getContent();
            } else if (!firstAddress.equals(result.getContent())) {
                foundDifferent = true;
                break;
            }
        }

        // With random routing, we should eventually get a different address
        assertTrue(foundDifferent, "Random routing should return different addresses over multiple calls");
    }
}
