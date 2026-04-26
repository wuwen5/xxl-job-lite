package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.JobExecutorInitParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.GsonTool;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by xuxueli on 17/5/10.
 */
@Controller
@RequestMapping("/api")
public class JobApiController {

    @Resource
    private AdminBiz adminBiz;

    /**
     * api
     *
     * @param uri
     * @param data
     * @return
     */
    @RequestMapping("/{uri}")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> api(
            HttpServletRequest request, @PathVariable("uri") String uri, @RequestBody(required = false) String data) {

        // valid
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
        }
        if (uri == null || uri.trim().isEmpty()) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
        }
        if (XxlJobAdminConfig.getAdminConfig().getAccessToken() != null
                && !XxlJobAdminConfig.getAdminConfig().getAccessToken().trim().isEmpty()
                && !XxlJobAdminConfig.getAdminConfig()
                        .getAccessToken()
                        .equals(request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN))) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "The access token is wrong.");
        }

        // services mapping
        switch (uri) {
            case "callback" -> {
                List<HandleCallbackParam> callbackParamList = GsonTool.fromJsonList(data, HandleCallbackParam.class);
                return adminBiz.callback(callbackParamList);
            }
            case "registry" -> {
                RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
                return adminBiz.registry(registryParam);
            }
            case "registryRemove" -> {
                RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
                return adminBiz.registryRemove(registryParam);
            }
            case "initJobInfo" -> {
                JobExecutorInitParam jobExecutorInitParam = GsonTool.fromJson(data, JobExecutorInitParam.class);
                return adminBiz.initJobInfo(jobExecutorInitParam);
            }
            default -> {
                return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping(" + uri + ") not found.");
            }
        }
    }
}
