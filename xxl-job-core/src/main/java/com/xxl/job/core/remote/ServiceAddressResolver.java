package com.xxl.job.core.remote;

/**
 * 服务地址解析器
 * @author wuwen
 */
public interface ServiceAddressResolver {

    /**
     * 将逻辑 URL 解析为最终访问 URL
     * @param rawUrl 逻辑 URL
     * @return 访问 URL
     */
    String resolve(String rawUrl);
}
