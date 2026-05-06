package org.apache.camel.sapagent4rest.service;

import com.alibaba.fastjson2.JSONObject;
import org.apache.camel.sapagent4rest.CustomConstants;
import org.apache.camel.sapagent4rest.entity.PreviewSapField;
import org.apache.camel.sapagent4rest.entity.SapFieldPreview;
import org.apache.camel.sapagent4rest.exception.CallSAPException;
import org.apache.camel.sapagent4rest.exception.RequestParamException;
import org.apache.camel.sapagent4rest.util.SapUtils;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectService {
    public void getPreviewData(Exchange exchange) throws Exception {
        String function = exchange.getIn().getHeader(CustomConstants.FUNCTION, String.class);
        String desName = exchange.getIn().getHeader("desName", String.class);
        Boolean isServer = exchange.getIn().getHeader("isServer", false, Boolean.class);

        if (StringUtils.isEmpty(function) || StringUtils.isEmpty(desName)) {
            throw new RequestParamException("FUNCTION or DESTINATION is empty!");
        }

        try {
            List<PreviewSapField> requestParameter = SapUtils.getRequestParameter(desName, function, isServer);
            List<PreviewSapField> responseParameter = SapUtils.getResponseParameter(desName, function, isServer);

            HashMap<String, Object> result = new HashMap<>();

            List<PreviewSapField> requestArray = new ArrayList<>();
            AtomicInteger deepReq = new AtomicInteger(0);
            result.put("requestData", getDemoData(requestParameter, requestArray, 1, deepReq));
            result.put("request", requestArray);
            result.put("deepReq", deepReq.get());

            List<PreviewSapField> responseArray = new ArrayList<>();
            AtomicInteger deepResp = new AtomicInteger(0);
            result.put("responseData", getDemoData(responseParameter, responseArray, 1, deepResp));
            result.put("response", responseArray);
            result.put("deepResp", deepResp.get());

            exchange.getIn().setBody(JSONObject.toJSONString(result));
        } catch (Exception e) {
            throw new CallSAPException(e.getMessage());
        }
    }

    public Map<String, Object> getDemoData(List<PreviewSapField> sapFieldList, List<PreviewSapField> arrayField,
                                           int level, AtomicInteger maxDepth) {
        if (level > maxDepth.get()) {
            maxDepth.set(level);
        }

        Map<String, Object> structure = new LinkedHashMap<>();
        for (PreviewSapField sapField : sapFieldList) {
            SapFieldPreview field = new SapFieldPreview();
            arrayField.add(field);
            if (level == 1) {
                field.setName(sapField.getName());
            } else {
                field.setSubName(sapField.getName());
            }
            field.setDesc(sapField.getDesc());

            String isTable = sapField.getIsTable();
            if ("value".equals(isTable)) {
                field.setType(sapField.getType());
                if ("BigDecimal".equals(sapField.getType())) {
                    field.setLengthStr(sapField.getLength() + "," + sapField.getDecimal());
                } else {
                    field.setLengthStr(String.valueOf(sapField.getLength()));
                }
                structure.put(sapField.getName(), getDemoValue(sapField));

            } else if ("table".equals(isTable)) {
                field.setType("Array");
                List<Object> list = new ArrayList<>();
                list.add(getDemoData(sapField.getTable(), arrayField, level + 1, maxDepth));
                structure.put(sapField.getName(), list);

            } else if ("object".equals(isTable)) {
                field.setType("Object");
                structure.put(sapField.getName(),
                        getDemoData(sapField.getTable(), arrayField, level + 1, maxDepth));
            }
        }
        return structure;
    }

    public Object getDemoValue(PreviewSapField sapField) {
        Object value = "value";
        switch (sapField.getType()) {
            case "BigDecimal":
                value = getBigDecimal(sapField.getDecimal()).toString();
                break;
            case "Number":
                value = "1";
                break;
            case "java.util.Date":
                value = SapDateFormats.demo(sapField.getLength());
                break;
            default:
                break;
        }
        return value;
    }

    public BigDecimal getBigDecimal(int fractionDigits) {
        // 创建一个初始值为0的BigDecimal
        BigDecimal value = BigDecimal.ONE;

        // 根据整数位数和小数位数设置其精度和舍入模式
        value = value.setScale(fractionDigits, RoundingMode.HALF_UP);

        // 返回结果
        return value;
    }
}
