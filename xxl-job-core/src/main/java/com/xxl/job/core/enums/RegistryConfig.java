package com.xxl.job.core.enums;

/**
 * Created by xuxueli on 17/5/10.
 */
public class RegistryConfig {

    public static final int BEAT_TIMEOUT = 30;
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    public enum RegistType {
        /**
         * 注册类型枚举
         */
        EXECUTOR,
        /**
         * 管理端
         */
        ADMIN
    }

}
