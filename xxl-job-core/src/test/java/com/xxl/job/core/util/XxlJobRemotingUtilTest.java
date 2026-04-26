package com.xxl.job.core.util;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.xxl.job.core.biz.model.ReturnT;
import org.junit.jupiter.api.Test;

/**
 * @author wuwen
 */
@WireMockTest(httpPort = 18760)
class XxlJobRemotingUtilTest {

    @Test
    public void should_return_success_when_http_request_succeeds() {

        stubFor(post(urlPathMatching("/api/registry"))
                .willReturn(ok().withBody("{\"code\":200,\"msg\":\"success\",\"content\":\"result\"}")));

        // given
        String url = "http://localhost:18760/api/registry";
        String accessToken = "test-token";
        int timeout = 5;
        Object requestObj = new Object();

        // when
        ReturnT<String> result = XxlJobRemotingUtil.postBody(url, accessToken, timeout, requestObj, String.class);

        // then
        assertThat(result.getCode()).isEqualTo(ReturnT.SUCCESS_CODE);
        assertThat(result.getMsg()).isEqualTo("success");
        assertThat(result.getContent()).isEqualTo("result");
    }
}
