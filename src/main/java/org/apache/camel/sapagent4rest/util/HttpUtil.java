package org.apache.camel.sapagent4rest.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class HttpUtil {

    static final int TIMEOUT_MS = 5 * 1000;

    private static final RestTemplate restTemplate;

    static {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        restTemplate = new RestTemplate(factory);
    }

    /**
     * 发送GET方式请求
     */
    public static String doGet(String url, Map<String, String> paramMap) {
        URI baseUri = null;
        try {
            baseUri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUri);
        if (paramMap != null) {
            paramMap.forEach((key, value) -> builder.queryParam(key, value));
        }
        URI uri = builder.build().toUri();
        return restTemplate.getForObject(uri, String.class);
    }

    /**
     * 发送POST方式请求，参数为键值对形式
     */
    public static String doPost(String url, Map<String, String> paramMap) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        if (paramMap != null) {
            paramMap.forEach(map::add);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        return response.getBody();

    }
}
