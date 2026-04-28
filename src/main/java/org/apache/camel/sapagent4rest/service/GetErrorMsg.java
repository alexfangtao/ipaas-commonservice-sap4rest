package org.apache.camel.sapagent4rest.service;

import org.apache.camel.sapagent4rest.FuseConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.sapagent4rest.exception.CallSAPException;
import org.apache.camel.sapagent4rest.exception.CustomInternalException;
import org.apache.camel.sapagent4rest.exception.RequestParamException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class GetErrorMsg {

    @Value("${camel.custom.sap.destination}")
    private String destinationConfig;

    public void getData(Exchange exchange) throws Exception {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        String statusCode = CustomInternalException.statusCode;
        if (exception instanceof RequestParamException) {
            statusCode = RequestParamException.STATUS_CODE;
        } else if (exception instanceof CallSAPException) {
            statusCode = CallSAPException.STATUS_CODE;
        } else {
            exception = new CustomInternalException(exception.getMessage(), exception);
        }

        String error = exception.getMessage();

        String svcNo = exchange.getProperty(FuseConstants.SVC_NO, String.class);
        if (StringUtils.isEmpty(svcNo)) {
            String desName = destinationConfig;
            exchange.setProperty(FuseConstants.SVC_NO, "SAP-" + desName);
        }


        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
        exchange.getIn().setBody(error);
    }

    public void errorHandler(Exchange exchange) throws Exception {
        Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        // Process
        if (Objects.nonNull(ex)) {
            log.error("日志保存接口报错:" + ex.toString());
        }
    }
}
