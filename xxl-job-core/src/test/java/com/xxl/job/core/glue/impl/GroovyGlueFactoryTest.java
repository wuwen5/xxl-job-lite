package com.xxl.job.core.glue.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.xxl.job.core.handler.IJobHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GroovyGlueFactory}: Groovy compilation, class caching and error handling.
 *
 * @author copilot
 */
class GroovyGlueFactoryTest {

    /** Groovy source that produces a valid {@link IJobHandler}. */
    private static final String VALID_HANDLER_SOURCE = "import com.xxl.job.core.handler.IJobHandler\n"
            + "class TestGroovyHandler extends IJobHandler {\n"
            + "    void execute() throws Exception {}\n"
            + "}\n";

    /** Groovy source whose compiled type is not an {@link IJobHandler}. */
    private static final String NON_HANDLER_SOURCE = "class NotAHandler { void run() {} }\n";

    private GroovyGlueFactory factory;

    @BeforeEach
    void setUp() {
        factory = new GroovyGlueFactory();
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void loadNewInstance_validSource_returnsIJobHandlerInstance() throws Exception {
        IJobHandler handler = factory.loadNewInstance(VALID_HANDLER_SOURCE);
        assertNotNull(handler);
        assertInstanceOf(IJobHandler.class, handler);
    }

    @Test
    void loadNewInstance_sameSourceTwice_returnsSameCompiledClass() throws Exception {
        IJobHandler first = factory.loadNewInstance(VALID_HANDLER_SOURCE);
        IJobHandler second = factory.loadNewInstance(VALID_HANDLER_SOURCE);

        // The class should be served from cache — both instances must be the same class.
        assertSame(first.getClass(), second.getClass());
        // But they must be distinct instances (prototype semantics).
        assertNotSame(first, second);
    }

    // -------------------------------------------------------------------------
    // Error / edge-case handling
    // -------------------------------------------------------------------------

    @Test
    void loadNewInstance_nullSource_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> factory.loadNewInstance(null));
    }

    @Test
    void loadNewInstance_emptySource_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> factory.loadNewInstance(""));
    }

    @Test
    void loadNewInstance_blankSource_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> factory.loadNewInstance("   "));
    }

    @Test
    void loadNewInstance_nonHandlerSource_throwsIllegalArgumentException() {
        // The compiled class is not an IJobHandler, so loadNewInstance must reject it.
        assertThrows(IllegalArgumentException.class, () -> factory.loadNewInstance(NON_HANDLER_SOURCE));
    }
}
