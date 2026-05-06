package org.apache.camel.sapagent4rest.service;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SapResponse {

    private static final String SAP_ANNOTATION_URI = "http://sap.fusesource.org/rfc";

    public void createResponse(Exchange exchange) throws Exception {
        Structure sapResponse = exchange.getIn().getBody(Structure.class);
        Map<String, Object> result = convertStructure(sapResponse);
        exchange.getIn().setBody(JSON.toJSONString(result));
    }

    private Map<String, Object> convertStructure(Structure structure) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (structure == null) {
            return result;
        }
        for (EStructuralFeature feature : structure.eClass().getEStructuralFeatures()) {
            String name = feature.getName();
            Object value = structure.get(name);
            result.put(name, convertValue(value, feature));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object convertValue(Object value, EStructuralFeature feature) {
        if (value == null) {
            return null;
        }
        if (value instanceof Structure) {
            return convertStructure((Structure) value);
        }
        if (value instanceof Table) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Structure row : ((Table<Structure>) value).getRows()) {
                rows.add(convertStructure(row));
            }
            return rows;
        }
        if (value instanceof Date) {
            return SapDateFormats.format((Date) value, readSapLength(feature));
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        if (value instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[]) value);
        }
        return value;
    }

    private int readSapLength(EStructuralFeature feature) {
        if (feature == null) {
            return 0;
        }
        EAnnotation annotation = feature.getEAnnotation(SAP_ANNOTATION_URI);
        if (annotation == null) {
            return 0;
        }
        String length = annotation.getDetails().get("length");
        if (length == null) {
            return 0;
        }
        try {
            return Integer.parseInt(length);
        } catch (NumberFormatException e) {
            log.warn("invalid SAP length annotation: {}", length);
            return 0;
        }
    }
}
