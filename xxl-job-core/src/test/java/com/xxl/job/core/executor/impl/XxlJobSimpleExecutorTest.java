package com.xxl.job.core.executor.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * XxlJobSimpleExecutor unit test
 *
 * @author wuwen
 */
class XxlJobSimpleExecutorTest {

    private static final String INIT_JOB_HANDLER_METHOD_REPOSITORY = "initJobHandlerMethodRepository";

    private Method initJobHandlerMethodRepositoryMethod;

    @BeforeEach
    void setUp() throws Exception {
        initJobHandlerMethodRepositoryMethod =
                XxlJobSimpleExecutor.class.getDeclaredMethod(INIT_JOB_HANDLER_METHOD_REPOSITORY, List.class);
        initJobHandlerMethodRepositoryMethod.setAccessible(true);
        clearJobHandlerRepository();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearJobHandlerRepository();
    }

    private void clearJobHandlerRepository() throws Exception {
        Field repositoryField = XxlJobExecutor.class.getDeclaredField("JOB_HANDLER_REPOSITORY");
        repositoryField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, IJobHandler> repository = (ConcurrentMap<String, IJobHandler>) repositoryField.get(null);
        repository.clear();
    }

    private void invokeInitJobHandlerMethodRepository(XxlJobSimpleExecutor executor, List<Object> beanList) {
        try {
            initJobHandlerMethodRepositoryMethod.invoke(executor, beanList);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testInitJobHandlerMethodRepository_withValidBean() throws Exception {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();
        TestJobBean bean = new TestJobBean();
        executor.setXxlJobBeanList(Collections.singletonList(bean));

        invokeInitJobHandlerMethodRepository(executor, executor.getXxlJobBeanList());

        IJobHandler handler = XxlJobExecutor.loadJobHandler("demoJob");
        assertNotNull(handler);
    }

    @Test
    void testInitJobHandlerMethodRepository_withMultipleMethods() throws Exception {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();
        TestJobBean bean = new TestJobBean();
        executor.setXxlJobBeanList(Collections.singletonList(bean));

        invokeInitJobHandlerMethodRepository(executor, executor.getXxlJobBeanList());

        assertNotNull(XxlJobExecutor.loadJobHandler("demoJob"));
        assertNotNull(XxlJobExecutor.loadJobHandler("anotherJob"));
    }

    @Test
    void testInitJobHandlerMethodRepository_withEmptyList() throws Exception {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();
        executor.setXxlJobBeanList(Collections.emptyList());

        invokeInitJobHandlerMethodRepository(executor, executor.getXxlJobBeanList());

        assertNull(XxlJobExecutor.loadJobHandler("demoJob"));
    }

    @Test
    void testInitJobHandlerMethodRepository_withNullList() throws Exception {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();

        invokeInitJobHandlerMethodRepository(executor, (List<Object>) null);

        assertNull(XxlJobExecutor.loadJobHandler("demoJob"));
    }

    @Test
    void testInitJobHandlerMethodRepository_withInitAndDestroy() throws Exception {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();
        TestJobBean bean = new TestJobBean();
        executor.setXxlJobBeanList(Collections.singletonList(bean));

        invokeInitJobHandlerMethodRepository(executor, executor.getXxlJobBeanList());

        IJobHandler handler = XxlJobExecutor.loadJobHandler("jobWithLifecycle");
        assertNotNull(handler);
        handler.init();
        assertTrue(bean.isInited());
        handler.destroy();
        assertTrue(bean.isDestroyed());
    }

    @Test
    void testInitJobHandlerMethodRepository_withDuplicateName() throws Exception {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();
        TestJobBean bean1 = new TestJobBean();
        TestJobBeanWithSameName bean2 = new TestJobBeanWithSameName();
        executor.setXxlJobBeanList(Arrays.asList(bean1, bean2));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            invokeInitJobHandlerMethodRepository(executor, executor.getXxlJobBeanList());
        });
        assertTrue(exception.getMessage().contains("naming conflicts"));
    }

    @Test
    void testInitJobHandlerMethodRepository_withEmptyName() throws Exception {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();
        TestJobBeanWithEmptyName bean = new TestJobBeanWithEmptyName();
        executor.setXxlJobBeanList(Collections.singletonList(bean));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            invokeInitJobHandlerMethodRepository(executor, executor.getXxlJobBeanList());
        });
        assertTrue(exception.getMessage().contains("name invalid"));
    }

    @Test
    void testInitJobHandlerMethodRepository_withInvalidInitMethod() throws Exception {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();
        TestJobBeanWithInvalidInit bean = new TestJobBeanWithInvalidInit();
        executor.setXxlJobBeanList(Collections.singletonList(bean));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            invokeInitJobHandlerMethodRepository(executor, executor.getXxlJobBeanList());
        });
        assertTrue(exception.getMessage().contains("initMethod invalid"));
    }

    @Test
    void testInitJobHandlerMethodRepository_withInvalidDestroyMethod() throws Exception {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();
        TestJobBeanWithInvalidDestroy bean = new TestJobBeanWithInvalidDestroy();
        executor.setXxlJobBeanList(Collections.singletonList(bean));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            invokeInitJobHandlerMethodRepository(executor, executor.getXxlJobBeanList());
        });
        assertTrue(exception.getMessage().contains("destroyMethod invalid"));
    }

    @Test
    void testInitJobHandlerMethodRepository_withMethodWithoutXxlJobAnnotation() throws Exception {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();
        TestJobBean bean = new TestJobBean();
        executor.setXxlJobBeanList(Collections.singletonList(bean));

        invokeInitJobHandlerMethodRepository(executor, executor.getXxlJobBeanList());

        assertNull(XxlJobExecutor.loadJobHandler("notAnnotated"));
    }

    @Test
    void testXxlJobBeanListGetterAndSetter() {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();
        List<Object> beanList = new ArrayList<>();
        beanList.add(new TestJobBean());

        executor.setXxlJobBeanList(beanList);

        assertEquals(beanList, executor.getXxlJobBeanList());
    }

    @Test
    void testDefaultXxlJobBeanListIsEmpty() {
        XxlJobSimpleExecutor executor = new XxlJobSimpleExecutor();

        assertNotNull(executor.getXxlJobBeanList());
        assertTrue(executor.getXxlJobBeanList().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Test helper classes
    // -------------------------------------------------------------------------

    static class TestJobBean {
        private boolean inited = false;
        private boolean destroyed = false;

        @XxlJob("demoJob")
        public void execute() {
            // do something
        }

        @XxlJob("anotherJob")
        public void anotherExecute() {
            // do something
        }

        @XxlJob(value = "jobWithLifecycle", init = "init", destroy = "destroy")
        public void executeWithLifecycle() {
            // do something
        }

        public void init() {
            inited = true;
        }

        public void destroy() {
            destroyed = true;
        }

        public boolean isInited() {
            return inited;
        }

        public boolean isDestroyed() {
            return destroyed;
        }

        public void notAnnotated() {
            // do something
        }
    }

    static class TestJobBeanWithSameName {
        @XxlJob("demoJob")
        public void execute() {
            // do something
        }
    }

    static class TestJobBeanWithEmptyName {
        @XxlJob("")
        public void execute() {
            // do something
        }
    }

    static class TestJobBeanWithInvalidInit {
        @XxlJob(value = "invalidInitJob", init = "notExistInitMethod")
        public void execute() {
            // do something
        }
    }

    static class TestJobBeanWithInvalidDestroy {
        @XxlJob(value = "invalidDestroyJob", destroy = "notExistDestroyMethod")
        public void execute() {
            // do something
        }
    }
}
