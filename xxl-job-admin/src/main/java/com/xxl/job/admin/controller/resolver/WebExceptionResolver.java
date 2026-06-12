package com.xxl.job.admin.controller.resolver;

import com.xxl.job.admin.core.exception.XxlJobException;
import com.xxl.job.admin.core.util.JacksonUtil;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * common exception resolver
 *
 * @author xuxueli 2016-1-6 19:22:18
 */
@Slf4j
@Component
public class WebExceptionResolver implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        if (!(ex instanceof XxlJobException)) {
            log.error("WebExceptionResolver:{}", ex);
        }

        // if json
        boolean isJson = false;
        if (handler instanceof HandlerMethod method) {
            ResponseBody responseBody = method.getMethodAnnotation(ResponseBody.class);
            if (responseBody != null) {
                isJson = true;
            }
        }

        // error result
        ReturnT<String> errorResult =
                new ReturnT<>(ReturnT.FAIL_CODE, ex.toString().replaceAll("\n", "<br/>"));

        // response
        ModelAndView mv = new ModelAndView();
        if (isJson) {
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().print(JacksonUtil.writeValueAsString(errorResult));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            return mv;
        } else {

            mv.addObject("exceptionMsg", errorResult.getMsg());
            mv.setViewName("/common/common.exception");
            return mv;
        }
    }
}
