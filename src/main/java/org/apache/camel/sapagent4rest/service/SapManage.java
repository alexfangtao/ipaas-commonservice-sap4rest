package org.apache.camel.sapagent4rest.service;

import com.sap.conn.jco.*;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.eclipse.emf.ecore.EPackage;
import org.fusesource.camel.component.sap.SapSynchronousRfcDestinationEndpoint;

public class SapManage {
    public static String convertSAPNamespaceToXMLName(String name) {
        if (name != null && name.contains("/")) {
            name = name.replaceFirst("/", "").replaceFirst("/", ":");
        }

        return name;
    }

    public void removePackage(Exchange exchange) throws Exception {
        String destination = exchange.getIn().getHeader("desName", String.class);
        String rfc = exchange.getIn().getHeader("rfc", String.class);
        removeCamelCache(exchange.getContext(), destination, rfc);
        exchange.getIn().setBody("success");
    }

    public static void removeCamelCache(CamelContext context, String destination, String rfc) throws JCoException {
        SapSynchronousRfcDestinationEndpoint endpoint = context.getEndpoint("sap-srfc-destination:" + destination + ":" + rfc, SapSynchronousRfcDestinationEndpoint.class);

        JCoRepository repository = JCoDestinationManager.getDestination((endpoint.getDestinationName())).getRepository();
        String nsURI = "http://sap.fusesource.org/rfc/" + repository.getName() + "/" + convertSAPNamespaceToXMLName(rfc);
        EPackage.Registry.INSTANCE.remove(nsURI);
        String[] cachedRecordMetaDataNames = repository.getCachedRecordMetaDataNames();

        for (String cachedRecordMetaDataName : cachedRecordMetaDataNames) {
            repository.removeRecordMetaDataFromCache(cachedRecordMetaDataName);
        }
        repository.removeFunctionTemplateFromCache(rfc);
    }


}
