package org.apache.camel.sapagent4rest.service;

import com.sgm.esb.ipaas.log.LogConstant;
import org.apache.camel.sapagent4rest.FuseConstants;
import org.apache.camel.Exchange;
import org.apache.camel.sapagent4rest.exception.RequestParamException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;


@Component
public class VerifyHeader {

    public void createRequest(Exchange exchange) throws Exception {
        String desName = exchange.getIn().getHeader(FuseConstants.DES_NAME, String.class);
        String rfc = exchange.getIn().getHeader(FuseConstants.RFC_LOWERCASE, String.class);
        String id = exchange.getIn().getHeader(FuseConstants.ID, String.class);

        exchange.setProperty(FuseConstants.DESTINATION, desName);
        exchange.setProperty(FuseConstants.RFC, rfc);

        String error = String.format("请求%s/%s/%s：", id,desName, rfc);
        String fromSys = exchange.getIn().getHeader(FuseConstants.HEADER_FROM, String.class);
        if (fromSys != null && !fromSys.isEmpty()) {
            fromSys = StringUtils.join(FuseConstants.HEADER_FROM, FuseConstants.STRING_COLON, fromSys);
            exchange.setProperty(FuseConstants.FROM_SYS, fromSys);
        } else {
            throw new RequestParamException(error + FuseConstants.HEADER_FROM + "不能为空!");
        }

        String traceId = exchange.getIn().getHeader(FuseConstants.TRACE_ID, String.class);
        if (traceId != null && !traceId.isEmpty()) {
            exchange.setProperty(LogConstant.traceId, traceId);
        }
        exchange.setProperty(FuseConstants.TO_SYS, "SAP " + desName);
    }
}
