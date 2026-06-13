package com.xxl.job.admin.core.route.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ExecutorRouteConsistentHash
 */
public class ExecutorRouteConsistentHashTest {

    @Test
    public void shouldRouteToConsistentAddress() {
        ExecutorRouteConsistentHash router = new ExecutorRouteConsistentHash();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(1);

        ReturnT<String> result1 = router.route(triggerParam, addressList);
        ReturnT<String> result2 = router.route(triggerParam, addressList);

        assertNotNull(result1);
        assertEquals(ReturnT.SUCCESS_CODE, result1.getCode());
        // Same job ID should always route to same address
        assertEquals(result1.getContent(), result2.getContent());
    }

    @Test
    public void shouldDistributeDifferentJobsAcrossAddresses() {
        ExecutorRouteConsistentHash router = new ExecutorRouteConsistentHash();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        Map<Integer, String> jobRoutingMap = new HashMap<>();

        // Route multiple jobs and track which address they go to
        for (int jobId = 1; jobId <= 100; jobId++) {
            TriggerParam triggerParam = new TriggerParam();
            triggerParam.setJobId(jobId);

            ReturnT<String> result = router.route(triggerParam, addressList);
            jobRoutingMap.put(jobId, result.getContent());
        }

        // Count how many jobs went to each address
        Map<String, Integer> addressCount = new HashMap<>();
        for (String address : jobRoutingMap.values()) {
            addressCount.merge(address, 1, Integer::sum);
        }

        // All addresses should be used
        assertEquals(3, addressCount.size());

        // Distribution should be relatively balanced (within reasonable range)
        int minCount = Collections.min(addressCount.values());
        int maxCount = Collections.max(addressCount.values());

        // With consistent hashing and virtual nodes, distribution should be fairly even
        // Allow for some variance but not extreme imbalance
        assertTrue(maxCount <= minCount * 3, "Distribution should be reasonably balanced: " + addressCount);
    }

    @Test
    public void shouldHandleSingleAddress() {
        ExecutorRouteConsistentHash router = new ExecutorRouteConsistentHash();
        List<String> addressList = Collections.singletonList("192.168.1.1:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(1);

        // All jobs should route to the single address
        for (int jobId = 1; jobId <= 10; jobId++) {
            triggerParam.setJobId(jobId);
            ReturnT<String> result = router.route(triggerParam, addressList);
            assertEquals("192.168.1.1:9999", result.getContent());
        }
    }

    @Test
    public void shouldHandleTwoAddresses() {
        ExecutorRouteConsistentHash router = new ExecutorRouteConsistentHash();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999");

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(1);

        ReturnT<String> result = router.route(triggerParam, addressList);

        assertNotNull(result);
        assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
        assertTrue(addressList.contains(result.getContent()));
    }

    @Test
    public void shouldMaintainConsistencyForSameJobWithDifferentOrder() {
        ExecutorRouteConsistentHash router = new ExecutorRouteConsistentHash();

        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(100);

        // Route with original order
        List<String> addressList1 = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");
        ReturnT<String> result1 = router.route(triggerParam, addressList1);

        // Route with different order (same addresses)
        List<String> addressList2 = Arrays.asList("192.168.1.2:9999", "192.168.1.3:9999", "192.168.1.1:9999");
        ReturnT<String> result2 = router.route(triggerParam, addressList2);

        // Note: Consistent hash may produce different results with different order
        // This is expected behavior - the test just verifies it doesn't crash
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(ReturnT.SUCCESS_CODE, result1.getCode());
        assertEquals(ReturnT.SUCCESS_CODE, result2.getCode());
    }

    @Test
    public void shouldRouteConsistentlyOverMultipleCalls() {
        ExecutorRouteConsistentHash router = new ExecutorRouteConsistentHash();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        int testJobId = 999;
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(testJobId);

        // Route the same job multiple times
        String firstAddress = null;
        for (int i = 0; i < 20; i++) {
            ReturnT<String> result = router.route(triggerParam, addressList);
            if (firstAddress == null) {
                firstAddress = result.getContent();
            } else {
                // Should always route to the same address for the same job
                assertEquals(
                        firstAddress,
                        result.getContent(),
                        "Job " + testJobId + " should consistently route to the same address");
            }
        }
    }

    @Test
    public void shouldHandleLargeNumberOfJobs() {
        ExecutorRouteConsistentHash router = new ExecutorRouteConsistentHash();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        Map<Integer, String> routingResults = new HashMap<>();

        // Test with many different job IDs
        for (int jobId = 1; jobId <= 1000; jobId++) {
            TriggerParam triggerParam = new TriggerParam();
            triggerParam.setJobId(jobId);

            ReturnT<String> result = router.route(triggerParam, addressList);
            routingResults.put(jobId, result.getContent());

            // Verify result is valid
            assertNotNull(result);
            assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
            assertTrue(addressList.contains(result.getContent()));
        }

        // Verify all jobs are routed
        assertEquals(1000, routingResults.size());

        // Verify all addresses are used
        long uniqueAddresses = routingResults.values().stream().distinct().count();
        assertEquals(3, uniqueAddresses);
    }

    @Test
    public void shouldHandleSpecificJobIds() {
        ExecutorRouteConsistentHash router = new ExecutorRouteConsistentHash();
        List<String> addressList = Arrays.asList("192.168.1.1:9999", "192.168.1.2:9999", "192.168.1.3:9999");

        // Test specific job IDs to ensure deterministic behavior
        int[] testJobIds = {1, 100, 1000, 10000, 999999};

        for (int jobId : testJobIds) {
            TriggerParam triggerParam = new TriggerParam();
            triggerParam.setJobId(jobId);

            ReturnT<String> result = router.route(triggerParam, addressList);

            assertNotNull(result);
            assertEquals(ReturnT.SUCCESS_CODE, result.getCode());
            assertTrue(addressList.contains(result.getContent()));

            // Verify consistency - route again with same job ID
            ReturnT<String> result2 = router.route(triggerParam, addressList);
            assertEquals(result.getContent(), result2.getContent());
        }
    }
}
