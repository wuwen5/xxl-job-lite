package com.xxl.job.core.glue.impl;

import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.handler.IJobHandler;
import groovy.lang.GroovyClassLoader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Groovy-backed GlueFactory implementation.
 * This class is only loaded when {@code groovy} is present on the classpath.
 *
 * @author xuxueli 2016-1-2 20:02:27
 */
@Slf4j
public class GroovyGlueFactory extends GlueFactory {

    /**
     * groovy class loader
     */
    private final GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

    private final ConcurrentMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

    /**
     * Load a new {@link IJobHandler} instance from the given Groovy source code.
     *
     * <p>The source is compiled via {@link GroovyClassLoader}, with compiled classes cached by the
     * MD5 hash of the source to avoid redundant recompilation. The resulting instance is injected
     * with any required services before being returned.
     *
     * @param codeSource the Groovy source code of the job handler; must not be {@code null} or blank
     * @return a freshly created {@link IJobHandler} instance
     * @throws IllegalArgumentException if the source is blank, the compiled class cannot be
     *     instantiated as an {@link IJobHandler}, or the compiled class is {@code null}
     * @throws Exception if compilation or instantiation fails
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
            byte[] md5 = MessageDigest.getInstance("MD5").digest(codeSource.getBytes(StandardCharsets.UTF_8));
            String md5Str = new BigInteger(1, md5).toString(16);

            return classCache.computeIfAbsent(md5Str, key -> groovyClassLoader.parseClass(codeSource));
        } catch (Exception e) {
            log.warn(
                    ">>>>>>>>>>> xxl-job, getCodeSourceClass md5 caching failed, parsing directly. Cause: {}",
                    e.getMessage());
            return groovyClassLoader.parseClass(codeSource);
        }
    }
}
