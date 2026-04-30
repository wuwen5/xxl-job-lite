package com.xxl.job.core.glue.impl;

import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.handler.IJobHandler;
import groovy.lang.GroovyClassLoader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Groovy-backed GlueFactory implementation.
 * This class is only loaded when {@code groovy} is present on the classpath.
 *
 * @author xuxueli 2016-1-2 20:02:27
 */
public class GroovyGlueFactory extends GlueFactory {
    private static final Logger logger = LoggerFactory.getLogger(GroovyGlueFactory.class);

    /**
     * groovy class loader
     */
    private GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

    private ConcurrentMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    /**
     * load new instance, prototype
     *
     * @param codeSource
     * @return
     * @throws Exception
     */
    @Override
    public IJobHandler loadNewInstance(String codeSource) throws Exception {
        if (codeSource != null && !codeSource.trim().isEmpty()) {
            Class<?> clazz = getCodeSourceClass(codeSource);
            if (clazz != null) {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                if (instance instanceof IJobHandler) {
                    this.injectService(instance);
                    return (IJobHandler) instance;
                } else {
                    throw new IllegalArgumentException(">>>>>>>>>>> xxl-glue, loadNewInstance error, "
                            + "cannot convert from instance[" + instance.getClass() + "] to IJobHandler");
                }
            }
        }
        throw new IllegalArgumentException(">>>>>>>>>>> xxl-glue, loadNewInstance error, instance is null");
    }

    private Class<?> getCodeSourceClass(String codeSource) {
        try {
            // md5
            byte[] md5 = MessageDigest.getInstance("MD5").digest(codeSource.getBytes());
            String md5Str = new BigInteger(1, md5).toString(16);

            Class<?> clazz = CLASS_CACHE.get(md5Str);
            if (clazz == null) {
                clazz = CLASS_CACHE.computeIfAbsent(md5Str, key -> groovyClassLoader.parseClass(codeSource));
            }
            return clazz;
        } catch (Exception e) {
            logger.warn(
                    ">>>>>>>>>>> xxl-job, getCodeSourceClass md5 caching failed, parsing directly. Cause: {}",
                    e.getMessage());
            return groovyClassLoader.parseClass(codeSource);
        }
    }
}
