package org.apache.camel.sapagent4rest.route;

import com.sap.conn.jco.ConversionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.sapagent4rest.config.CustomProperties;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.sapagent4rest.exception.CallSAPException;
import org.apache.camel.sapagent4rest.exception.RequestParamException;
import org.apache.camel.sapagent4rest.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Camel route definitions.
 */
@Component
@Slf4j
public class Routes extends RouteBuilder {

    @Autowired
    CustomProperties prop;

    @Value("${camel.custom.rest.token}")
    private String restToken;

    @Override
    public void configure() throws Exception {

        //RESTful入口
        from(prop.getRestful()).to("direct:mainRoute");

        from("direct:mainRoute").id("mainRoute")
                .doTry()
                    .bean(CacheService.class,"verifyConfig")
                    .bean(VerifyHeader.class, "createRequest")
                    .toD("ipaas-logger:RestRequest?code=code1&fromApp=${exchangeProperty.X-FROM-SYS-ID}&toApp=${header.desName} Agent")
                    .to("direct:callSap").id("callSap")
                .doCatch(Exception.class)
                    .bean(GetErrorMsg.class, "getData")
                    .toD("ipaas-logger:SAPException?code=code9&fromApp=${header.desName} Agent&toApp=${exchangeProperty.X-FROM-SYS-ID}")
                    .log("### Process error:  ${body}")
                .end()
                .toD("ipaas-logger:RestResponse?code=code4&fromApp=${header.desName} Agent&toApp=${exchangeProperty.X-FROM-SYS-ID}")
                .end();

        from("direct:callSap")
                .bean(SapRequest.class, "createRequest")
                .toD("ipaas-logger:Request2SAP?code=code2&fromApp=${header.desName} Agent&toApp=${exchangeProperty.X-TO-SYS-ID}")
                .doTry()
                .toD("sap-srfc-destination:${exchangeProperty.DESTINATION}:${exchangeProperty.RFC}")
                .doCatch(ConversionException.class)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    throw new RequestParamException(exception.getMessage());
                })
                .doCatch(Exception.class)
                .process(exchange -> {
                    Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    throw new CallSAPException(exception.getMessage());
                })
                .end()
                .toD("ipaas-logger:ResponseFromSAP?code=code3&fromApp=${exchangeProperty.X-TO-SYS-ID}&toApp=${header.desName} Agent")
                .bean(SapResponse.class, "createResponse")
                .end();

        from("rest:post:operation/preview/{desName}/{function}")
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain; charset=UTF-8"))
                .bean(SelectService.class,"getPreviewData")
                .end();

        from(prop.getDelCache())
                .process(p->{
                    String token = p.getIn().getHeader("token", String.class);
                    if (StringUtils.isEmpty(token) || !restToken.equals(token)) {
                        throw new RequestParamException("请传入token");
                    }
                })
                .bean(SapManage.class,"removePackage")
                .end();
    }

}
