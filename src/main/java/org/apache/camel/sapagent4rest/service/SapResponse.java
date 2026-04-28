package org.apache.camel.sapagent4rest.service;

import org.apache.camel.Exchange;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import com.alibaba.fastjson2.JSON;

public class SapResponse {

    public void createRequest(Exchange exchange) throws Exception {
        Structure sapResponse = exchange.getIn().getBody(Structure.class);

        String json = JSON.toJSONString(sapResponse, "yyyy-MM-dd");
        exchange.getIn().setBody(json);
    }
}
