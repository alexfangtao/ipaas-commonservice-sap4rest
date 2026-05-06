package org.apache.camel.sapagent4rest.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

public class CallSAPException extends RuntimeException {
    public static final int STATUS_CODE = 500;
    public CallSAPException(String message) {
        super(StringUtils.join("调用SAP异常:", message));
    }

    public CallSAPException(String message, Throwable cause) {
        super(StringUtils.join("调用SAP异常:", message), cause);
    }

}
