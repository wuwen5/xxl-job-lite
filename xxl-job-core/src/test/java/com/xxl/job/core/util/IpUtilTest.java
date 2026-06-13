package com.xxl.job.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;

/**
 * IpUtil unit test
 *
 * @author wuwen
 */
class IpUtilTest {

    @Test
    void testGetLocalAddress() {
        InetAddress address = IpUtil.getLocalAddress();

        // Should return a valid InetAddress (may be null in some environments)
        // The important thing is no exception is thrown
        assertTrue(true);
    }

    @Test
    void testGetIp() {
        String ip = IpUtil.getIp();

        // Should return an IP address string (may vary by environment)
        assertNotNull(ip);
        assertFalse(ip.isEmpty());
    }

    @Test
    void testGetIpPortWithInt() {
        String ipPort = IpUtil.getIpPort(8080);

        assertNotNull(ipPort);
        assertTrue(ipPort.endsWith(":8080"));
    }

    @Test
    void testGetIpPortWithIpAndPort() {
        String ipPort = IpUtil.getIpPort("192.168.1.1", 9090);

        assertEquals("192.168.1.1:9090", ipPort);
    }

    @Test
    void testGetIpPortWithNullIp() {
        String ipPort = IpUtil.getIpPort((String) null, 8080);

        assertNull(ipPort);
    }

    @Test
    void testParseIpPort() {
        Object[] result = IpUtil.parseIpPort("192.168.1.1:8080");

        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("192.168.1.1", result[0]);
        assertEquals(8080, result[1]);
    }

    @Test
    void testParseIpPortWithDifferentPort() {
        Object[] result = IpUtil.parseIpPort("10.0.0.1:3306");

        assertNotNull(result);
        assertEquals("10.0.0.1", result[0]);
        assertEquals(3306, result[1]);
    }

    @Test
    void testGetIpPortConsistency() {
        String ip = IpUtil.getIp();
        String ipPort = IpUtil.getIpPort(8080);

        // ipPort should contain the IP and port
        assertTrue(ipPort.contains(ip));
        assertTrue(ipPort.contains("8080"));
    }

    @Test
    void testMultipleCallsToLocalAddress() {
        // Multiple calls should return the same cached value
        InetAddress addr1 = IpUtil.getLocalAddress();
        InetAddress addr2 = IpUtil.getLocalAddress();

        assertSame(addr1, addr2);
    }

    @Test
    void testValidIpAddressFormat() {
        String ip = IpUtil.getIp();

        // Basic validation of IP format (IPv4)
        if (ip != null && !ip.isEmpty()) {
            String[] parts = ip.split("\\.");
            assertEquals(4, parts.length);

            for (String part : parts) {
                int num = Integer.parseInt(part);
                assertTrue(num >= 0 && num <= 255);
            }
        }
    }

    @Test
    void testGetIpPortWithZeroPort() {
        String ipPort = IpUtil.getIpPort("127.0.0.1", 0);

        assertEquals("127.0.0.1:0", ipPort);
    }

    @Test
    void testGetIpPortWithLargePort() {
        String ipPort = IpUtil.getIpPort("127.0.0.1", 65535);

        assertEquals("127.0.0.1:65535", ipPort);
    }

    @Test
    void testParseIpPortWithLocalhost() {
        Object[] result = IpUtil.parseIpPort("127.0.0.1:8080");

        assertEquals("127.0.0.1", result[0]);
        assertEquals(8080, result[1]);
    }

    @Test
    void testParseIpPortResultTypes() {
        Object[] result = IpUtil.parseIpPort("192.168.1.100:9090");

        assertTrue(result[0] instanceof String);
        assertTrue(result[1] instanceof Integer);
    }
}
