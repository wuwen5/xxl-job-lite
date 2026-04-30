package com.xxl.job.core.glue.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.handler.IJobHandler;
import java.lang.reflect.Field;
import javax.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;

/**
 * Unit tests for {@link SpringGlueFactory}: Spring bean injection via {@code injectService} and
 * inherited Groovy compilation from {@link GroovyGlueFactory}.
 *
 * @author copilot
 */
class SpringGlueFactoryTest {

    /** Groovy source that produces a valid {@link IJobHandler}. */
    private static final String VALID_HANDLER_SOURCE = "import com.xxl.job.core.handler.IJobHandler\n"
            + "class SpringGroovyHandler extends IJobHandler {\n"
            + "    void execute() throws Exception {}\n"
            + "}\n";

    private SpringGlueFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        factory = new SpringGlueFactory();
        setApplicationContext(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        setApplicationContext(null);
    }

    /** Reflectively sets the private static {@code applicationContext} field. */
    private static void setApplicationContext(ApplicationContext ctx) throws Exception {
        Field f = XxlJobSpringExecutor.class.getDeclaredField("applicationContext");
        f.setAccessible(true);
        f.set(null, ctx);
    }

    // -------------------------------------------------------------------------
    // injectService — no-op cases
    // -------------------------------------------------------------------------

    @Test
    void injectService_nullInstance_doesNotThrow() {
        assertDoesNotThrow(() -> factory.injectService(null));
    }

    @Test
    void injectService_noApplicationContext_doesNotThrow() {
        // ApplicationContext is null (cleared in setUp), so injectService must short-circuit.
        assertDoesNotThrow(() -> factory.injectService(new Object()));
    }

    @Test
    void injectService_noApplicationContext_leavesFieldUnset() throws Exception {
        TargetWithResource target = new TargetWithResource();
        factory.injectService(target);
        // No context available — field must remain null.
        assertNull(target.myService);
    }

    // -------------------------------------------------------------------------
    // injectService — @Resource injection
    // -------------------------------------------------------------------------

    @Test
    void injectService_resourceByFieldName_injectsBean() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        String bean = "injected-by-name";
        when(ctx.getBean("myService")).thenReturn(bean);
        setApplicationContext(ctx);

        TargetWithResource target = new TargetWithResource();
        factory.injectService(target);

        assertEquals(bean, target.myService);
    }

    @Test
    void injectService_resourceWithExplicitName_injectsBeanByThatName() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        String bean = "explicit-bean";
        when(ctx.getBean("explicitName")).thenReturn(bean);
        setApplicationContext(ctx);

        TargetWithNamedResource target = new TargetWithNamedResource();
        factory.injectService(target);

        assertEquals(bean, target.myService);
    }

    @Test
    void injectService_resourceByType_injectsBean_whenNameLookupFails() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        String bean = "type-resolved-bean";
        when(ctx.getBean("myService")).thenThrow(new RuntimeException("not found by name"));
        when(ctx.getBean(String.class)).thenReturn(bean);
        setApplicationContext(ctx);

        TargetWithResource target = new TargetWithResource();
        factory.injectService(target);

        assertEquals(bean, target.myService);
    }

    // -------------------------------------------------------------------------
    // injectService — @Autowired injection
    // -------------------------------------------------------------------------

    @Test
    void injectService_autowiredByType_injectsBean() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        String bean = "autowired-bean";
        when(ctx.getBean(String.class)).thenReturn(bean);
        setApplicationContext(ctx);

        TargetWithAutowired target = new TargetWithAutowired();
        factory.injectService(target);

        assertEquals(bean, target.myService);
    }

    @Test
    void injectService_autowiredWithQualifier_injectsBeanByQualifierName() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        String bean = "qualified-bean";
        when(ctx.getBean("myQualifier")).thenReturn(bean);
        setApplicationContext(ctx);

        TargetWithQualifier target = new TargetWithQualifier();
        factory.injectService(target);

        assertEquals(bean, target.myService);
    }

    // -------------------------------------------------------------------------
    // injectService — static fields must be skipped
    // -------------------------------------------------------------------------

    @Test
    void injectService_staticField_isSkipped() throws Exception {
        TargetWithStaticField.staticService = null;
        ApplicationContext ctx = mock(ApplicationContext.class);
        setApplicationContext(ctx);

        factory.injectService(new TargetWithStaticField());

        // Static field must remain untouched; no getBean call should occur.
        assertNull(TargetWithStaticField.staticService);
        verifyNoInteractions(ctx);
    }

    // -------------------------------------------------------------------------
    // loadNewInstance (inherited from GroovyGlueFactory)
    // -------------------------------------------------------------------------

    @Test
    void loadNewInstance_compilesThroughSpringFactory() throws Exception {
        IJobHandler handler = factory.loadNewInstance(VALID_HANDLER_SOURCE);
        assertNotNull(handler);
        assertInstanceOf(IJobHandler.class, handler);
    }

    @Test
    void loadNewInstance_withApplicationContext_injectsServicesAfterCompile() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        setApplicationContext(ctx);

        // The compiled handler has no annotated fields, so getBean is never called.
        IJobHandler handler = factory.loadNewInstance(VALID_HANDLER_SOURCE);

        assertNotNull(handler);
        verifyNoInteractions(ctx);
    }

    // -------------------------------------------------------------------------
    // Static target helper classes
    // -------------------------------------------------------------------------

    static class TargetWithResource {
        @Resource
        String myService;
    }

    static class TargetWithNamedResource {
        @Resource(name = "explicitName")
        String myService;
    }

    static class TargetWithAutowired {
        @Autowired
        String myService;
    }

    static class TargetWithQualifier {
        @Autowired
        @Qualifier("myQualifier")
        String myService;
    }

    static class TargetWithStaticField {
        @Resource
        static String staticService;
    }
}
