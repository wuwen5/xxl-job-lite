package com.xxl.job.admin.controller;

import com.xxl.job.admin.AbstractTest;
import com.xxl.job.admin.core.util.I18nUtil;
import freemarker.template.TemplateModelException;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

public class AbstractSpringMvcTest extends AbstractTest {

    @Autowired
    private WebApplicationContext applicationContext;

    protected MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        this.mockMvc =
                MockMvcBuilders.webAppContextSetup(this.applicationContext).build();
    }

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @PostConstruct
    public void init() throws TemplateModelException {
        freeMarkerConfigurer.getConfiguration().setSharedVariable("I18nUtil", new I18nUtil());
    }
}
