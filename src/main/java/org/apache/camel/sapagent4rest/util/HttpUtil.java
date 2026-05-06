package org.apache.camel.sapagent4rest.util;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

public class HttpUtil {

    static final int TIMEOUT_MS = 5 * 1000;

    private static final RestClient restClient;

    static {
        // 1. 连接池管理器
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(50);

        // 2. 请求级超时配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(TIMEOUT_MS))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(TIMEOUT_MS))
                .setResponseTimeout(Timeout.ofMilliseconds(TIMEOUT_MS))
                .build();

        // 3. 构建支持连接池的 HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();

        // 4. 注入 RestClient
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * 发送GET方式请求
     */
    public static String doGet(String url, Map<String, String> paramMap) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        if (paramMap != null) {
            paramMap.forEach(builder::queryParam);
        }
        URI uri = builder.build().toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);
    }

    /**
     * 发送POST方式请求，参数为键值对形式
     */
    public static String doPost(String url, Map<String, String> paramMap) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        if (paramMap != null) {
            paramMap.forEach(map::add);
        }

        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(map)
                .retrieve()
                .body(String.class);
    }
}
