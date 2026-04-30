package com.xxl.job.core.glue;

import static org.junit.jupiter.api.Assertions.*;

import com.xxl.job.core.glue.impl.GroovyGlueFactory;
import com.xxl.job.core.glue.impl.SpringGlueFactory;
import com.xxl.job.core.handler.IJobHandler;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GlueFactory} classpath detection and factory selection.
 *
 * @author copilot
 */
class GlueFactoryTest {

    @Test
    void getInstance_returnsNonNull() {
        assertNotNull(GlueFactory.getInstance());
    }

    @Test
    void refreshInstance_type0_returnsGroovyGlueFactory() {
        // Groovy is on the classpath (optional compile dependency), so type-0 should
        // create a GroovyGlueFactory.
        GlueFactory.refreshInstance(0);
        assertInstanceOf(GroovyGlueFactory.class, GlueFactory.getInstance());
    }

    @Test
    void refreshInstance_type1_returnsSpringGlueFactory() {
        // type-1 selects the Spring-aware factory when Groovy is present.
        GlueFactory.refreshInstance(1);
        assertInstanceOf(SpringGlueFactory.class, GlueFactory.getInstance());

        // Restore to default
        GlueFactory.refreshInstance(0);
    }

    @Test
    void baseGlueFactory_loadNewInstance_throwsUnsupportedOperationException() {
        GlueFactory base = new GlueFactory();
        assertThrows(UnsupportedOperationException.class, () -> base.loadNewInstance("anything"));
    }

    @Test
    void baseGlueFactory_injectService_isNoOp() {
        // Should complete without throwing for any input, including null
        GlueFactory base = new GlueFactory();
        assertDoesNotThrow(() -> base.injectService(null));
        assertDoesNotThrow(() -> base.injectService(new Object()));
    }

    /** Minimal IJobHandler implementation used only inside GlueFactory tests. */
    static class MinimalJobHandler extends IJobHandler {
        @Override
        public void execute() {}
    }
}
