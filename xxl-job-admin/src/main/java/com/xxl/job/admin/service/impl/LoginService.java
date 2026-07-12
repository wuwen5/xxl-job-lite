package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.CookieUtil;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobUserDao;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author xuxueli 2019-05-04 22:13:264
 */
@Service
public class LoginService {

    public static final String LOGIN_IDENTITY_KEY = "XXL_JOB_LOGIN_IDENTITY";

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long REMEMBER_ME_MULTIPLIER = 30L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${xxl.job.login.token-secret:}")
    private String configuredSecret;

    @Value("${xxl.job.login.token-expire-seconds:86400}")
    private long tokenExpireSeconds;

    private byte[] secretKey;

    @Resource
    private XxlJobUserDao xxlJobUserDao;

    @PostConstruct
    public void init() {
        if (configuredSecret != null && !configuredSecret.trim().isEmpty()) {
            secretKey = configuredSecret.trim().getBytes(StandardCharsets.UTF_8);
        } else {
            byte[] generated = new byte[32];
            SECURE_RANDOM.nextBytes(generated);
            secretKey = generated;
        }
    }

    // ---------------------- token tool ----------------------

    private String makeToken(int userId, long expireAt) {
        String payload = userId + ":" + expireAt;
        String signature = sign(payload);
        return base64UrlEncode(payload.getBytes(StandardCharsets.UTF_8)) + "." + signature;
    }

    private int parseToken(String token) {
        if (token == null) {
            return -1;
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            return -1;
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return -1;
        }

        String providedSig = parts[1];

        String expectedSig = sign(payload);
        if (!constantTimeEquals(expectedSig, providedSig)) {
            return -1;
        }

        String[] payloadParts = payload.split(":");
        if (payloadParts.length != 2) {
            return -1;
        }

        long expireAt;
        try {
            int userId = Integer.parseInt(payloadParts[0]);
            expireAt = Long.parseLong(payloadParts[1]);
            if (System.currentTimeMillis() > expireAt) {
                return -1;
            }
            return userId;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.getUrlEncoder().withoutPadding().encode(rawHmac), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign token", e);
        }
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    // ---------------------- login tool, with cookie and db ----------------------

    public ReturnT<String> login(HttpServletResponse response, String username, String password, boolean ifRemember) {

        // param
        if (username == null
                || username.trim().isEmpty()
                || password == null
                || password.trim().isEmpty()) {
            return new ReturnT<>(500, I18nUtil.getString("login_param_empty"));
        }

        // valid password
        XxlJobUser xxlJobUser = xxlJobUserDao.loadByUserName(username);
        if (xxlJobUser == null) {
            return new ReturnT<>(500, I18nUtil.getString("login_param_unvalid"));
        }
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!passwordMd5.equals(xxlJobUser.getPassword())) {
            return new ReturnT<>(500, I18nUtil.getString("login_param_unvalid"));
        }

        long expireSeconds = ifRemember ? tokenExpireSeconds * REMEMBER_ME_MULTIPLIER : tokenExpireSeconds;
        long expireAt = System.currentTimeMillis() + expireSeconds * 1000;
        String loginToken = makeToken(xxlJobUser.getId(), expireAt);

        // do login (cookie for backward compatibility)
        CookieUtil.set(response, LOGIN_IDENTITY_KEY, loginToken, ifRemember);

        // return token in response header for frontend
        response.setHeader("Authorization", "Bearer " + loginToken);

        return new ReturnT<>(loginToken);
    }

    /**
     * logout
     *
     * @param request
     * @param response
     */
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
        return ReturnT.SUCCESS;
    }

    /**
     * if login
     *
     * @param request
     * @return
     */
    public XxlJobUser ifLogin(HttpServletRequest request, HttpServletResponse response) {
        // 1. try to get token from Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 2. fallback to cookie
        boolean isCookieAuth = false;
        if (token == null) {
            token = CookieUtil.getValue(request, LOGIN_IDENTITY_KEY);
            isCookieAuth = true;
        }

        if (token != null) {
            int userId = parseToken(token);
            if (userId > 0) {
                XxlJobUser dbUser = xxlJobUserDao.loadById(userId);
                if (dbUser != null) {
                    return dbUser;
                }
            }

            if (isCookieAuth) {
                // invalid or expired token - clear cookie
                logout(request, response);
            }
        }
        return null;
    }
}
