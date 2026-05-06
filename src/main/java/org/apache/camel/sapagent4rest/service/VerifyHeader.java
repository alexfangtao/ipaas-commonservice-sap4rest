package org.apache.camel.sapagent4rest.service;

import com.sgm.esb.ipaas.log.LogConstant;
import org.apache.camel.sapagent4rest.CustomConstants;
import org.apache.camel.Exchange;
import org.apache.camel.sapagent4rest.exception.RequestParamException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;


@Component
public class VerifyHeader {

    public void createRequest(Exchange exchange) throws Exception {
        String desName = exchange.getIn().getHeader(CustomConstants.DES_NAME, String.class);
        String rfc = exchange.getIn().getHeader(CustomConstants.RFC_LOWERCASE, String.class);
        String id = exchange.getIn().getHeader(CustomConstants.ID, String.class);

        exchange.setProperty(CustomConstants.DESTINATION, desName);
        exchange.setProperty(CustomConstants.RFC, rfc);

        String error = String.format("请求%s/%s/%s：", id,desName, rfc);
        String fromSys = exchange.getIn().getHeader(CustomConstants.HEADER_FROM, String.class);
        if (fromSys != null && !fromSys.isEmpty()) {
            fromSys = StringUtils.join(CustomConstants.HEADER_FROM, CustomConstants.STRING_COLON, fromSys);
            exchange.setProperty(CustomConstants.FROM_SYS, fromSys);
        } else {
            throw new RequestParamException(error + CustomConstants.HEADER_FROM + "不能为空!");
        }

        String traceId = exchange.getIn().getHeader(CustomConstants.TRACE_ID, String.class);
        if (traceId != null && !traceId.isEmpty()) {
            exchange.setProperty(LogConstant.traceId, traceId);
        }
        exchange.setProperty(CustomConstants.TO_SYS, "SAP " + desName);
    }
}
