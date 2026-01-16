package com.xxl.job.admin.controller;

import com.xxl.job.admin.AbstractTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

public class AbstractSpringMvcTest extends AbstractTest {

  @Autowired
  private WebApplicationContext applicationContext;
  protected MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.applicationContext).build();
  }

}
