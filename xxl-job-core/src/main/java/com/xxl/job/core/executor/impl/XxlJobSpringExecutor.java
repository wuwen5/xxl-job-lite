package com.xxl.job.core.executor.impl;

import com.xxl.job.core.biz.model.JobInfoParam;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.thread.ExecutorRegistryThread;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * xxl-job executor (for spring)
 *
 * @author xuxueli 2018-11-01 09:24:52
 */
public class XxlJobSpringExecutor extends XxlJobExecutor
        implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobSpringExecutor.class);

    // start
    @Override
    public void afterSingletonsInstantiated() {

        // init JobHandler Repository
        /*initJobHandlerRepository(applicationContext);*/

        // init JobHandler Repository (for method)
        List<JobInfoParam> jobInfoParams = initJobHandlerMethodRepository(applicationContext);
        ExecutorRegistryThread.getInstance().initJobInfoInitParams(jobInfoParams);

        // refresh GlueFactory
        GlueFactory.refreshInstance(1);

        // super start
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // destroy
    @Override
    public void destroy() {
        super.destroy();
    }

    private List<JobInfoParam> initJobHandlerMethodRepository(ApplicationContext applicationContext) {
        List<JobInfoParam> list = new ArrayList<>();
        if (applicationContext == null) {
            return list;
        }
        // init job handler from method
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanDefinitionName : beanDefinitionNames) {

            // get bean
            Object bean;
            Lazy onBean = applicationContext.findAnnotationOnBean(beanDefinitionName, Lazy.class);
            if (onBean != null) {
                logger.debug("xxl-job annotation scan, skip @Lazy Bean:{}", beanDefinitionName);
                continue;
            } else {
                bean = applicationContext.getBean(beanDefinitionName);
            }

            // filter method
            // referred to ：org.springframework.context.event.EventListenerMethodProcessor.processBean
            Map<Method, XxlJob> annotatedMethods = null;
            try {
                annotatedMethods =
                        MethodIntrospector.selectMethods(bean.getClass(), (MethodIntrospector.MetadataLookup<XxlJob>)
                                method -> AnnotatedElementUtils.findMergedAnnotation(method, XxlJob.class));
            } catch (Throwable ex) {
                logger.error("xxl-job method-jobhandler resolve error for bean[{}].", beanDefinitionName, ex);
            }
            if (annotatedMethods == null || annotatedMethods.isEmpty()) {
                continue;
            }

            // generate and regist method job handler
            for (Map.Entry<Method, XxlJob> methodXxlJobEntry : annotatedMethods.entrySet()) {
                Method executeMethod = methodXxlJobEntry.getKey();
                XxlJob xxlJob = methodXxlJobEntry.getValue();
                // regist
                registJobHandler(xxlJob, bean, executeMethod);

                if (xxlJob != null && (!xxlJob.cron().isEmpty() || xxlJob.fixedRate() > 0)) {
                    JobInfoParam infoInitParam = new JobInfoParam();
                    infoInitParam.setExecutorParam(xxlJob.param());
                    infoInitParam.setJobDesc(xxlJob.desc());
                    infoInitParam.setExecutorHandler(xxlJob.value());
                    infoInitParam.setCron(xxlJob.cron());
                    infoInitParam.setFixedRate(xxlJob.fixedRate());
                    list.add(infoInitParam);
                }
            }
        }
        return list;
    }

    // ---------------------- applicationContext ----------------------
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        XxlJobSpringExecutor.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
