package com.xxl.job.admin.controller;

import com.xxl.job.admin.AbstractTest;
import com.xxl.job.admin.core.util.I18nUtil;
import freemarker.template.TemplateModelException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

@AutoConfigureMockMvc
public abstract class AbstractSpringMvcTest extends AbstractTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @PostConstruct
    public void init() throws TemplateModelException {
        freeMarkerConfigurer.getConfiguration().setSharedVariable("I18nUtil", new I18nUtil());
    }
}
