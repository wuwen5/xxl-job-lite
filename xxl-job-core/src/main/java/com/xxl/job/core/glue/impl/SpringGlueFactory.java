package com.xxl.job.core.glue.impl;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Spring-aware GlueFactory that extends {@link GroovyGlueFactory} to additionally inject Spring
 * beans into Groovy job handler instances after compilation.
 *
 * @author xuxueli 2018-11-01
 */
@Slf4j
public class SpringGlueFactory extends GroovyGlueFactory {

    /**
     * Inject Spring-managed beans into the fields of the given Groovy job handler instance.
     *
     * <p>Fields annotated with {@link Resource} are resolved by bean name (using the field name as
     * a fallback), then by type. Fields annotated with {@link Autowired} are resolved by type,
     * optionally qualified via {@link Qualifier}. Fields that cannot be resolved are silently
     * skipped.
     *
     * @param instance the job handler instance whose fields are to be injected; ignored if
     *     {@code null} or if no Spring {@code ApplicationContext} is available
     */
    @Override
    public void injectService(Object instance) {
        if (instance == null) {
            return;
        }

        if (XxlJobSpringExecutor.getApplicationContext() == null) {
            return;
        }

        Field[] fields = instance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Object fieldBean = null;
            // with bean-id, bean could be found by both @Resource and @Autowired, or bean could only be found by
            // @Autowired

            if (AnnotationUtils.getAnnotation(field, Resource.class) != null) {
                try {
                    Resource resource = AnnotationUtils.getAnnotation(field, Resource.class);
                    if (resource.name() != null && resource.name().length() > 0) {
                        fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(resource.name());
                    } else {
                        fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(field.getName());
                    }
                } catch (Exception e) {
                }
                if (fieldBean == null) {
                    fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(field.getType());
                }
            } else if (AnnotationUtils.getAnnotation(field, Autowired.class) != null) {
                Qualifier qualifier = AnnotationUtils.getAnnotation(field, Qualifier.class);
                if (qualifier != null
                        && qualifier.value() != null
                        && qualifier.value().length() > 0) {
                    fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(qualifier.value());
                } else {
                    fieldBean = XxlJobSpringExecutor.getApplicationContext().getBean(field.getType());
                }
            }

            if (fieldBean != null) {
                field.setAccessible(true);
                try {
                    field.set(instance, fieldBean);
                } catch (IllegalArgumentException e) {
                    log.error(e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
}
