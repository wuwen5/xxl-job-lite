package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.admin.service.impl.LoginService;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * index controller
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
public class IndexController {

    @Resource
    private XxlJobService xxlJobService;

    @Resource
    private LoginService loginService;

    @GetMapping("/")
    public String index(Model model) {

        Map<String, Object> dashboardMap = xxlJobService.dashboardInfo();
        model.addAllAttributes(dashboardMap);

        return "index";
    }

    @GetMapping("/chartInfo")
    @ResponseBody
    public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {
        return xxlJobService.chartInfo(startDate, endDate);
    }

    @GetMapping("/toLogin")
    @PermissionLimit(limit = false)
    public ModelAndView toLogin(HttpServletRequest request, HttpServletResponse response, ModelAndView modelAndView) {
        if (loginService.ifLogin(request, response) != null) {
            modelAndView.setView(new RedirectView("/", true, false));
            return modelAndView;
        }
        return new ModelAndView("login");
    }

    @PostMapping(value = "login")
    @ResponseBody
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
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response) {
        return loginService.logout(request, response);
    }

    @GetMapping("/help")
    public String help() {
        return "help";
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }
}
