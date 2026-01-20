package com.xxl.job.core.util;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.remote.ServiceAddressResolver;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * @author xuxueli 2018-11-25 00:55:31
 */
public class XxlJobRemotingUtil {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(XxlJobRemotingUtil.class);
    
    public static final String XXL_JOB_ACCESS_TOKEN = "XXL-JOB-ACCESS-TOKEN";

    private static volatile ServiceAddressResolver addressResolver = rawUrl -> rawUrl;

    private static final SSLContext SSL_CONTEXT = createSslContext();

    private static final SSLConnectionSocketFactory SSL_SOCKET_FACTORY = new SSLConnectionSocketFactory(SSL_CONTEXT, NoopHostnameVerifier.INSTANCE);
    
    public static void setAddressResolver(ServiceAddressResolver resolver) {
        if (resolver != null) {
            addressResolver = resolver;
        }
    }
    
    
    /**
     * post 
     *
     * @param url 请求地址
     * @param accessToken 访问令牌
     * @param timeout           by second
     * @param requestObj 请求对象
     * @param returnTargClassOfT 返回对象类型
     * @return ReturnT<T> 返回结果
     */
    public static <T> ReturnT<T> postBody(String url, String accessToken, int timeout, Object requestObj, Class<T> returnTargClassOfT) {

        String resolvedUrl = addressResolver.resolve(url);

        HttpPost post = new HttpPost(resolvedUrl);
        post.addHeader("Content-Type", "application/json;charset=UTF-8");
        post.addHeader("Accept-Charset", "application/json;charset=UTF-8");

        if (accessToken != null && !accessToken.isEmpty()) {
            post.setHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN, accessToken);
        }

        try (CloseableHttpClient client = getHttpClient(timeout)) {

            if (requestObj != null) {
                String json = GsonTool.toJson(requestObj);
                post.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
            }

            try (CloseableHttpResponse response = client.execute(post)) {

                int status = response.getStatusLine().getStatusCode();
                if (status != ReturnT.SUCCESS_CODE) {
                    return ReturnT.ofFail(
                            "xxl-job remoting fail, status=" + status +
                                    ", url=" + resolvedUrl);
                }

                String resultJson = EntityUtils.toString(
                        response.getEntity(), StandardCharsets.UTF_8);

                try {
                    return GsonTool.fromReturnJson(resultJson, returnTargClassOfT);
                } catch (Exception e) {
                    LOGGER.error("xxl-job remoting (url={}) response content invalid({}).", url, resultJson, e);
                    return ReturnT.ofFail("xxl-job remoting (url="+url+") response content invalid("+ resultJson +").");
                }
            }

        } catch (Exception e) {
            return ReturnT.ofFail(
                    "xxl-job remoting error(" + e.getMessage() + "), url=" + resolvedUrl);
        }
    }

    private static CloseableHttpClient getHttpClient(int timeoutSeconds) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeoutSeconds * 1000)
                .setSocketTimeout(timeoutSeconds * 1000)
                .setConnectionRequestTimeout(timeoutSeconds * 1000)
                .build();

        return HttpClients.custom()
                .setSSLSocketFactory(SSL_SOCKET_FACTORY)
                .setDefaultRequestConfig(config)
                .build();
    }

    private static SSLContext createSslContext() {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(
                                X509Certificate[] chain, String authType) {}
                        @Override
                        public void checkServerTrusted(
                                X509Certificate[] chain, String authType) {}
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            }, new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
