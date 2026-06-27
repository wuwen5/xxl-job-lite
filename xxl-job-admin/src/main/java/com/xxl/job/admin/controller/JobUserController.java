package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobUserDao;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xuxueli 2019-05-04 16:39:50
 */
@RestController
@RequestMapping("/admin-api/v1/user")
public class JobUserController {

    private static final int USERNAME_PASSWORD_MIN_LENGTH = 4;
    private static final int USERNAME_PASSWORD_MAX_LENGTH = 20;

    @Resource
    private XxlJobUserDao xxlJobUserDao;

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @GetMapping
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(
            @RequestParam(required = false, defaultValue = "0") int start,
            @RequestParam(required = false, defaultValue = "10") int length,
            String username,
            int role) {

        // page list
        List<XxlJobUser> list = xxlJobUserDao.pageList(start, length, username, role);
        int listCount = xxlJobUserDao.pageListCount(start, length, username, role);

        // filter
        if (list != null && !list.isEmpty()) {
            for (XxlJobUser item : list) {
                item.setPassword(null);
            }
        }

        // package result
        Map<String, Object> maps = new HashMap<>(3);
        maps.put("recordsTotal", listCount);
        // 过滤后的总记录数
        maps.put("recordsFiltered", listCount);
        maps.put("data", list);
        return maps;
    }

    @PostMapping
    @PermissionLimit(adminuser = true)
    public ReturnT<String> add(@RequestBody XxlJobUser xxlJobUser) {

        // valid username
        if (!StringUtils.hasText(xxlJobUser.getUsername())) {
            return new ReturnT<>(
                    ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input") + I18nUtil.getString("user_username"));
        }
        xxlJobUser.setUsername(xxlJobUser.getUsername().trim());
        if (!(xxlJobUser.getUsername().length() >= USERNAME_PASSWORD_MIN_LENGTH
                && xxlJobUser.getUsername().length() <= USERNAME_PASSWORD_MAX_LENGTH)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
        }
        // valid password
        if (!StringUtils.hasText(xxlJobUser.getPassword())) {
            return new ReturnT<>(
                    ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input") + I18nUtil.getString("user_password"));
        }
        xxlJobUser.setPassword(xxlJobUser.getPassword().trim());
        if (!(xxlJobUser.getPassword().length() >= USERNAME_PASSWORD_MIN_LENGTH
                && xxlJobUser.getPassword().length() <= USERNAME_PASSWORD_MAX_LENGTH)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
        }
        // md5 password
        xxlJobUser.setPassword(
                DigestUtils.md5DigestAsHex(xxlJobUser.getPassword().getBytes()));

        // check repeat
        XxlJobUser existUser = xxlJobUserDao.loadByUserName(xxlJobUser.getUsername());
        if (existUser != null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("user_username_repeat"));
        }

        // write
        xxlJobUserDao.save(xxlJobUser);
        return ReturnT.SUCCESS;
    }

    @PutMapping("/{id}")
    @PermissionLimit(adminuser = true)
    public ReturnT<String> update(@RequestBody XxlJobUser xxlJobUser, @PathVariable int id) {

        xxlJobUser.setId(id);

        // avoid opt login seft
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser();
        if (loginUser.getUsername().equals(xxlJobUser.getUsername())) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        // valid password
        if (StringUtils.hasText(xxlJobUser.getPassword())) {
            xxlJobUser.setPassword(xxlJobUser.getPassword().trim());
            if (!(xxlJobUser.getPassword().length() >= USERNAME_PASSWORD_MIN_LENGTH
                    && xxlJobUser.getPassword().length() <= USERNAME_PASSWORD_MAX_LENGTH)) {
                return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
            }
            // md5 password
            xxlJobUser.setPassword(
                    DigestUtils.md5DigestAsHex(xxlJobUser.getPassword().getBytes()));
        } else {
            xxlJobUser.setPassword(null);
        }

        // write
        xxlJobUserDao.update(xxlJobUser);
        return ReturnT.SUCCESS;
    }

    @DeleteMapping("/{id}")
    @PermissionLimit(adminuser = true)
    public ReturnT<String> remove(@PathVariable int id) {

        // avoid opt login seft
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser();
        if (loginUser.getId() == id) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }

        xxlJobUserDao.delete(id);
        return ReturnT.SUCCESS;
    }

    @PutMapping("/me/password")
    public ReturnT<String> updatePwd(@RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String password = body.get("password");

        // valid
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return new ReturnT<>(
                    ReturnT.FAIL.getCode(),
                    I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
        }
        if (password == null || password.trim().isEmpty()) {
            return new ReturnT<>(
                    ReturnT.FAIL.getCode(),
                    I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
        }
        password = password.trim();
        if (!(password.length() >= USERNAME_PASSWORD_MIN_LENGTH && password.length() <= USERNAME_PASSWORD_MAX_LENGTH)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit") + "[4-20]");
        }

        // md5 password
        String md5OldPassword = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        // valid old pwd
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser();
        XxlJobUser existUser = xxlJobUserDao.loadByUserName(loginUser.getUsername());
        if (!md5OldPassword.equals(existUser.getPassword())) {
            return new ReturnT<>(
                    ReturnT.FAIL.getCode(),
                    I18nUtil.getString("change_pwd_field_oldpwd") + I18nUtil.getString("system_unvalid"));
        }

        // write new
        existUser.setPassword(md5Password);
        xxlJobUserDao.update(existUser);

        return ReturnT.SUCCESS;
    }
}
