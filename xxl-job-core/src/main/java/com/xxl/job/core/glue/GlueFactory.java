package com.xxl.job.core.glue;

import com.xxl.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * glue factory, product class/object by name
 *
 * @author xuxueli 2016-1-2 20:02:27
 */
public class GlueFactory {
    private static final Logger logger = LoggerFactory.getLogger(GlueFactory.class);

    private static final String GROOVY_CLASS_LOADER_CLASS = "groovy.lang.GroovyClassLoader";
    private static final String GROOVY_GLUE_FACTORY_CLASS = "com.xxl.job.core.glue.impl.GroovyGlueFactory";
    private static final String SPRING_GLUE_FACTORY_CLASS = "com.xxl.job.core.glue.impl.SpringGlueFactory";

    private static GlueFactory glueFactory = createInstance(false);

    public static GlueFactory getInstance() {
        return glueFactory;
    }

    public static void refreshInstance(int type) {
        if (type == 0) {
            glueFactory = createInstance(false);
        } else if (type == 1) {
            glueFactory = createInstance(true);
        }
    }

    private static GlueFactory createInstance(boolean springEnabled) {
        if (isGroovyAvailable()) {
            String className = springEnabled ? SPRING_GLUE_FACTORY_CLASS : GROOVY_GLUE_FACTORY_CLASS;
            try {
                return (GlueFactory)
                        Class.forName(className).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                logger.warn(
                        ">>>>>>>>>>> xxl-job, failed to create Groovy-capable GlueFactory [{}], "
                                + "falling back to base factory. Cause: {}",
                        className,
                        e.getMessage());
            }
        }
        return new GlueFactory();
    }

    /**
     * Returns {@code true} if the Groovy runtime is available on the classpath.
     */
    private static boolean isGroovyAvailable() {
        try {
            Class.forName(GROOVY_CLASS_LOADER_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (LinkageError e) {
            logger.warn(
                    ">>>>>>>>>>> xxl-job, Groovy detected but not usable; falling back to base factory. Cause: {}",
                    e.getMessage());
            return false;
        }
    }

    /**
     * load new instance, prototype
     *
     * @param codeSource
     * @return
     * @throws Exception
     */
    public IJobHandler loadNewInstance(String codeSource) throws Exception {
        throw new UnsupportedOperationException(">>>>>>>>>>> xxl-job, GLUE(Groovy) is not supported. "
                + "Please add 'org.apache.groovy:groovy' to your dependencies to enable Groovy GLUE jobs.");
    }

    /**
     * inject service of bean field
     *
     * @param instance
     */
    public void injectService(Object instance) {
        // do something
    }
}
