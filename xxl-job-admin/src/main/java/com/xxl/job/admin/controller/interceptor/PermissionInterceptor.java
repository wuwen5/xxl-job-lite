package com.xxl.job.admin.controller.interceptor;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.service.impl.LoginService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * 权限拦截
 *
 * @author xuxueli 2015-12-12 18:09:04
 */
@Component
public class PermissionInterceptor implements AsyncHandlerInterceptor {

    @Resource
    private LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        if (!(handler instanceof HandlerMethod method)) {
            // proceed with the next interceptor
            return true;
        }

        // if need login
        boolean needLogin = true;
        boolean needAdminuser = false;
        PermissionLimit permission = method.getMethodAnnotation(PermissionLimit.class);
        if (permission != null) {
            needLogin = permission.limit();
            needAdminuser = permission.adminuser();
        }

        if (needLogin) {
            XxlJobUser loginUser = loginService.ifLogin(request, response);
            if (loginUser == null) {
                response.setStatus(302);
                response.setHeader("location", request.getContextPath() + "/toLogin");
                return false;
            }
            if (needAdminuser && loginUser.getRole() != 1) {
                throw new RuntimeException(I18nUtil.getString("system_permission_limit"));
            }
            // set loginUser, with request
            request.setAttribute(LoginService.LOGIN_IDENTITY_KEY, loginUser);
        }

        // proceed with the next interceptor
        return true;
    }

    // -------------------- permission tool --------------------

    /**
     * get loginUser
     *
     * @param request
     * @return
     */
    public static XxlJobUser getLoginUser(HttpServletRequest request) {
        // get loginUser, with request
        return (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
    }

    /**
     * valid permission by JobGroup
     *
     * @param request
     * @param jobGroup
     */
    public static void validJobGroupPermission(HttpServletRequest request, int jobGroup) {
        XxlJobUser loginUser = getLoginUser(request);
        if (!loginUser.validPermission(jobGroup)) {
            throw new RuntimeException(
                    I18nUtil.getString("system_permission_limit") + "[username=" + loginUser.getUsername() + "]");
        }
    }

    /**
     * filter XxlJobGroup by role
     *
     * @param request
     * @param jobGroupList_all
     * @return
     */
    public static List<XxlJobGroup> filterJobGroupByRole(
            HttpServletRequest request, List<XxlJobGroup> jobGroupList_all) {
        List<XxlJobGroup> jobGroupList = new ArrayList<>();
        if (jobGroupList_all != null && !jobGroupList_all.isEmpty()) {
            XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
            if (loginUser.getRole() == 1) {
                jobGroupList = jobGroupList_all;
            } else {
                List<String> groupIdStrs = new ArrayList<>();
                if (loginUser.getPermission() != null
                        && !loginUser.getPermission().trim().isEmpty()) {
                    groupIdStrs = Arrays.asList(loginUser.getPermission().trim().split(","));
                }
                for (XxlJobGroup groupItem : jobGroupList_all) {
                    if (groupIdStrs.contains(String.valueOf(groupItem.getId()))) {
                        jobGroupList.add(groupItem);
                    }
                }
            }
        }
        return jobGroupList;
    }
}
