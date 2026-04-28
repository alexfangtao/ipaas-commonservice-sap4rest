package org.apache.camel.sapagent4rest.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class RequestParamException extends RuntimeException {

    public static final String STATUS_CODE = "400";
    public RequestParamException(String message) {
        super(StringUtils.join("请求参数异常:", message));
    }

    public RequestParamException(String message, Throwable cause) {
        super(StringUtils.join("请求参数异常:", message), cause);
        log.error(StringUtils.join("请求参数异常:", message), cause);
    }
}
