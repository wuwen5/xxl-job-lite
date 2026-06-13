package com.xxl.job.core.handler.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * MethodJobHandler unit test
 *
 * @author wuwen
 */
class MethodJobHandlerTest {

    @Test
    void testConstructor() throws Exception {
        TestTarget target = new TestTarget();
        Method method = target.getClass().getMethod("execute");

        MethodJobHandler handler = new MethodJobHandler(target, method, null, null);

        assertNotNull(handler);
    }

    @Test
    void testExecuteWithNoParams() throws Exception {
        TestTarget target = new TestTarget();
        Method method = target.getClass().getMethod("execute");

        MethodJobHandler handler = new MethodJobHandler(target, method, null, null);

        // Execute should not throw exception
        handler.execute();

        assertTrue(target.isExecuted());
    }

    @Test
    void testExecuteWithParams() throws Exception {
        TestTargetWithParams target = new TestTargetWithParams();
        Method method = target.getClass().getMethod("execute", String.class, Integer.class);

        MethodJobHandler handler = new MethodJobHandler(target, method, null, null);

        // Execute with params (will pass null values)
        handler.execute();

        assertTrue(target.isExecuted());
    }

    @Test
    void testInit() throws Exception {
        TestTarget target = new TestTarget();
        Method executeMethod = target.getClass().getMethod("execute");
        Method initMethod = target.getClass().getMethod("init");

        MethodJobHandler handler = new MethodJobHandler(target, executeMethod, initMethod, null);

        // Call init
        handler.init();

        assertTrue(target.isInited());
    }

    @Test
    void testInitWithNullInitMethod() throws Exception {
        TestTarget target = new TestTarget();
        Method method = target.getClass().getMethod("execute");

        MethodJobHandler handler = new MethodJobHandler(target, method, null, null);

        // Init with null initMethod should not throw exception
        handler.init();

        assertFalse(target.isInited());
    }

    @Test
    void testDestroy() throws Exception {
        TestTarget target = new TestTarget();
        Method executeMethod = target.getClass().getMethod("execute");
        Method destroyMethod = target.getClass().getMethod("destroy");

        MethodJobHandler handler = new MethodJobHandler(target, executeMethod, null, destroyMethod);

        // Call destroy
        handler.destroy();

        assertTrue(target.isDestroyed());
    }

    @Test
    void testDestroyWithNullDestroyMethod() throws Exception {
        TestTarget target = new TestTarget();
        Method method = target.getClass().getMethod("execute");

        MethodJobHandler handler = new MethodJobHandler(target, method, null, null);

        // Destroy with null destroyMethod should not throw exception
        handler.destroy();

        assertFalse(target.isDestroyed());
    }

    @Test
    void testFullLifecycle() throws Exception {
        TestTarget target = new TestTarget();
        Method executeMethod = target.getClass().getMethod("execute");
        Method initMethod = target.getClass().getMethod("init");
        Method destroyMethod = target.getClass().getMethod("destroy");

        MethodJobHandler handler = new MethodJobHandler(target, executeMethod, initMethod, destroyMethod);

        // Full lifecycle
        handler.init();
        handler.execute();
        handler.destroy();

        assertTrue(target.isInited());
        assertTrue(target.isExecuted());
        assertTrue(target.isDestroyed());
    }

    @Test
    void testToString() throws Exception {
        TestTarget target = new TestTarget();
        Method method = target.getClass().getMethod("execute");

        MethodJobHandler handler = new MethodJobHandler(target, method, null, null);

        String result = handler.toString();

        assertNotNull(result);
        assertTrue(result.contains(target.getClass().getSimpleName()));
        assertTrue(result.contains("execute"));
    }

    @Test
    void testMultipleExecutes() throws Exception {
        TestTarget target = new TestTarget();
        Method method = target.getClass().getMethod("execute");

        MethodJobHandler handler = new MethodJobHandler(target, method, null, null);

        // Execute multiple times
        handler.execute();
        handler.execute();
        handler.execute();

        assertEquals(3, target.getExecuteCount());
    }

    @Test
    void testExecuteWithException() throws Exception {
        TestTargetWithException target = new TestTargetWithException();
        Method method = target.getClass().getMethod("execute");

        MethodJobHandler handler = new MethodJobHandler(target, method, null, null);

        // Execute should propagate exception
        assertThrows(Exception.class, () -> handler.execute());
    }

    @Test
    void testInitWithException() throws Exception {
        TestTargetWithException target = new TestTargetWithException();
        Method executeMethod = target.getClass().getMethod("execute");
        Method initMethod = target.getClass().getMethod("init");

        MethodJobHandler handler = new MethodJobHandler(target, executeMethod, initMethod, null);

        // Init should propagate exception
        assertThrows(Exception.class, () -> handler.init());
    }

    @Test
    void testDestroyWithException() throws Exception {
        TestTargetWithException target = new TestTargetWithException();
        Method executeMethod = target.getClass().getMethod("execute");
        Method destroyMethod = target.getClass().getMethod("destroy");

        MethodJobHandler handler = new MethodJobHandler(target, executeMethod, null, destroyMethod);

        // Destroy should propagate exception
        assertThrows(Exception.class, () -> handler.destroy());
    }

    @Test
    void testDifferentTargets() throws Exception {
        TestTarget target1 = new TestTarget();
        TestTarget target2 = new TestTarget();

        Method method1 = target1.getClass().getMethod("execute");
        Method method2 = target2.getClass().getMethod("execute");

        MethodJobHandler handler1 = new MethodJobHandler(target1, method1, null, null);
        MethodJobHandler handler2 = new MethodJobHandler(target2, method2, null, null);

        // Different handlers should work independently
        handler1.execute();
        handler2.execute();

        assertTrue(target1.isExecuted());
        assertTrue(target2.isExecuted());
    }

    @Test
    void testPrivateMethodExecution() throws Exception {
        TestTargetWithPrivateMethod target = new TestTargetWithPrivateMethod();
        Method method = target.getClass().getDeclaredMethod("privateExecute");
        method.setAccessible(true);

        MethodJobHandler handler = new MethodJobHandler(target, method, null, null);

        // Should be able to execute private method (if accessible)
        handler.execute();

        assertTrue(target.isExecuted());
    }

    // -------------------------------------------------------------------------
    // Test helper classes
    // -------------------------------------------------------------------------

    static class TestTarget {
        private boolean inited = false;
        private boolean executed = false;
        private boolean destroyed = false;
        private int executeCount = 0;

        public void init() {
            inited = true;
        }

        public void execute() {
            executed = true;
            executeCount++;
        }

        public void destroy() {
            destroyed = true;
        }

        public boolean isInited() {
            return inited;
        }

        public boolean isExecuted() {
            return executed;
        }

        public boolean isDestroyed() {
            return destroyed;
        }

        public int getExecuteCount() {
            return executeCount;
        }
    }

    static class TestTargetWithParams {
        private boolean executed = false;

        public void execute(String param1, Integer param2) {
            executed = true;
        }

        public boolean isExecuted() {
            return executed;
        }
    }

    static class TestTargetWithException {
        public void init() throws Exception {
            throw new Exception("Init error");
        }

        public void execute() throws Exception {
            throw new Exception("Execute error");
        }

        public void destroy() throws Exception {
            throw new Exception("Destroy error");
        }
    }

    static class TestTargetWithPrivateMethod {
        private boolean executed = false;

        private void privateExecute() {
            executed = true;
        }

        public boolean isExecuted() {
            return executed;
        }
    }
}
