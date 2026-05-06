package org.apache.camel.sapagent4rest.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

public class CustomInternalException extends RuntimeException {
    public static final int statusCode = 500;
    public CustomInternalException(String message) {
        super(StringUtils.join("公共服务内部异常:", message));
    }

    public CustomInternalException(String message, Throwable cause) {
        super(StringUtils.join("公共服务内部异常:", message), cause);
    }


}
