package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.admin.service.impl.LoginService;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * index controller
 * @author xuxueli 2015-12-19 16:13:16
 */
@RestController
@RequestMapping("/admin-api/v1")
public class IndexController {

    @Resource
    private XxlJobService xxlJobService;

    @Resource
    private LoginService loginService;

    @GetMapping("/userinfo")
    public ReturnT<Map<String, Object>> userinfo() {
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser();
        Map<String, Object> data = new HashMap<>();
        data.put("id", loginUser.getId());
        data.put("username", loginUser.getUsername());
        data.put("role", loginUser.getRole());
        data.put("permission", loginUser.getPermission());
        return new ReturnT<>(data);
    }

    @GetMapping("/dashboard")
    public ReturnT<Map<String, Object>> dashboard() {
        return new ReturnT<>(xxlJobService.dashboardInfo());
    }

    @GetMapping("/chart")
    public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {
        return xxlJobService.chartInfo(startDate, endDate);
    }

    @PostMapping(value = "login")
    @PermissionLimit(limit = false)
    public ReturnT<String> loginDo(
            HttpServletRequest request,
            HttpServletResponse response,
            String userName,
            String password,
            String ifRemember) {
        boolean ifRem = ifRemember != null && !ifRemember.trim().isEmpty() && "on".equals(ifRemember);
        return loginService.login(request, response, userName, password, ifRem);
    }

    @PostMapping(value = "logout")
    @PermissionLimit(limit = false)
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response) {
        return loginService.logout(request, response);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }
}
