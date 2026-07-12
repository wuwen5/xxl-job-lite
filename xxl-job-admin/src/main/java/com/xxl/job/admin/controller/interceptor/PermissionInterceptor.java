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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
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

                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                try {
                    response.getWriter().write("{\"code\":401,\"msg\":\"未登录或登录已过期\",\"content\":null}");
                } catch (Exception ignored) {
                    // ignore
                }
                return false;
            }
            if (needAdminuser && loginUser.getRole() != 1) {
                throw new SecurityException(I18nUtil.getString("system_permission_limit"));
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
     */
    public static XxlJobUser getLoginUser() {
        // get loginUser, with request
        return (XxlJobUser) RequestContextHolder.currentRequestAttributes()
                .getAttribute(LoginService.LOGIN_IDENTITY_KEY, RequestAttributes.SCOPE_REQUEST);
    }

    /**
     * valid permission by JobGroup
     */
    public static void validJobGroupPermission(int jobGroup) {
        XxlJobUser loginUser = getLoginUser();
        if (!loginUser.validPermission(jobGroup)) {
            throw new SecurityException(
                    I18nUtil.getString("system_permission_limit") + "[username=" + loginUser.getUsername() + "]");
        }
    }

    /**
     * filter XxlJobGroup by role
     */
    public static List<XxlJobGroup> filterJobGroupByRole(List<XxlJobGroup> jobGroupListAll) {
        List<XxlJobGroup> jobGroupList = new ArrayList<>();
        if (jobGroupListAll != null && !jobGroupListAll.isEmpty()) {
            XxlJobUser loginUser = PermissionInterceptor.getLoginUser();
            if (loginUser.getRole() == 1) {
                jobGroupList = jobGroupListAll;
            } else {
                List<String> groupIdStrs = new ArrayList<>();
                if (loginUser.getPermission() != null
                        && !loginUser.getPermission().trim().isEmpty()) {
                    groupIdStrs = Arrays.asList(loginUser.getPermission().trim().split(","));
                }
                for (XxlJobGroup groupItem : jobGroupListAll) {
                    if (groupIdStrs.contains(String.valueOf(groupItem.getId()))) {
                        jobGroupList.add(groupItem);
                    }
                }
            }
        }
        return jobGroupList;
    }
}
