package org.apache.camel.sapagent4rest.route;

import com.sap.conn.jco.ConversionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.sapagent4rest.config.ESBProperties;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.sapagent4rest.exception.CallSAPException;
import org.apache.camel.sapagent4rest.exception.RequestParamException;
import org.apache.camel.sapagent4rest.service.*;
import org.apache.camel.sapagent4rest.util.RsaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Camel route definitions.
 */
@Component
@Slf4j
public class Routes extends RouteBuilder {

    @Autowired
    ESBProperties prop;

    @Override
    public void configure() throws Exception {
        restConfiguration().bindingMode(RestBindingMode.json);

        //RESTful入口
        from(prop.getRestful()).to("direct:mainRoute");

        from("direct:mainRoute").id("mainRoute")
                .doTry()
                    .bean(CacheService.class,"verifyConfig")
                    .bean(VerifyHeader.class)
                    .toD("ipaas-logger:RestRequest?code=code1&from=${exchangeProperty.X-FROM-SYS-ID}&to=${header.desName} Agent")
                    .to("direct:callSap").id("callSap")
                .doCatch(Exception.class)
                    .bean(GetErrorMsg.class, "getData")
                    .toD("ipaas-logger:SAPException?code=code9&from=${header.desName} Agent&to=${exchangeProperty.X-FROM-SYS-ID}")
                    .log("### Process error:  ${body}")
                .end()
                .toD("ipaas-logger:RestResponse?code=code4&from=${header.desName} Agent&to=${exchangeProperty.X-FROM-SYS-ID}")
                .end();

        from("direct:callSap")
                .bean(SapRequest.class)
                .toD("ipaas-logger:Request2SAP?code=code2&from=${header.desName} Agent&to=${exchangeProperty.X-TO-SYS-ID}")
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
                .toD("ipaas-logger:ResponseFromSAP?code=code3&from=${exchangeProperty.X-TO-SYS-ID}&to=${header.desName} Agent")
                .bean(SapResponse.class)
                .end();

        from("rest:post:operation/preview/{desName}/{function}")
                .setHeader("Access-Control-Allow-Origin", constant("*"))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain; charset=UTF-8"))
                .bean(SelectService.class,"getPreviewData")
                .end();

        from(prop.getDelCache())
                .bean(SapManage.class,"removePackage")
                .end();

        from("rest:get:queryQuitInterfaceEncryption").process(p->{
            p.getIn().setBody(RsaUtil.getQuitInterfaceEncryption() == null ? "null" : RsaUtil.getQuitInterfaceEncryption().toString());
        }).end();

        from("rest:get:setQuitInterfaceEncryption").process(p->{
            Boolean quitInterfaceEncryption = p.getIn().getHeader("quitInterfaceEncryption", Boolean.class);
            RsaUtil.setQuitInterfaceEncryption(quitInterfaceEncryption);
        }).end();
    }

    @Override
    public void addTemplatedRoutesToCamelContext(CamelContext context) throws Exception {

    }

    @Override
    public Set<String> updateRoutesToCamelContext(CamelContext context) throws Exception {
        return null;
    }
}
