package org.apache.camel.sapagent4rest.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoRepository;
import org.apache.camel.sapagent4rest.FuseConstants;
import org.apache.camel.sapagent4rest.entity.SapField;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.sapagent4rest.exception.CallSAPException;
import org.apache.camel.sapagent4rest.exception.RequestParamException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.fusesource.camel.component.sap.SapSynchronousRfcDestinationEndpoint;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.fusesource.camel.component.sap.model.rfc.impl.StructureImpl;
import org.fusesource.camel.component.sap.util.RfcUtil;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class SapRequest {
    public void createRequest(Exchange exchange) throws Exception {

        String destination = exchange.getProperty(FuseConstants.DESTINATION, String.class);
        String rfc = exchange.getProperty(FuseConstants.RFC, String.class);
        //获取请求传递得json报文
        String body = exchange.getIn().getBody(String.class);
        JSONObject inputData = JSONObject.parseObject(body);
        Structure requestEndpoint = null;
        SapSynchronousRfcDestinationEndpoint endpoint = null;
        try {
            //获取RFC在SAP组件中得端点对象
            endpoint = exchange.getContext()
                    .getEndpoint("sap-srfc-destination:" + destination + ":" + rfc, SapSynchronousRfcDestinationEndpoint.class);
            //获取RFC请求报文结构对象
            requestEndpoint = endpoint.createRequest();
        } catch (Exception exception) {
            throw new CallSAPException(exception.getMessage(), exception);
        }

        if (requestEndpoint == null) {
            throw new CallSAPException("SAP " + destination + "实例不存在该RFC!");
        }

        // 解析字段的结构转换成自定义得类型
        List<SapField> sapFieldList = getSapStructure(rfc, endpoint, requestEndpoint);

        // 遍历structure赋值
        setStructure(inputData, requestEndpoint, sapFieldList);
        exchange.getIn().setBody(requestEndpoint);
    }

    public void setStructure(JSONObject inputData, Structure requestEndpoint, List<SapField> sapFieldList) throws Exception {
        Map<String, Object> inputDataUpper = mapKeyUpper(inputData);
        for (SapField sapField : sapFieldList) {
            if (!inputDataUpper.containsKey(sapField.getName())) {
                throw new RequestParamException(FuseConstants.ERROR_MSG + sapField.getName());
            }
            Object inputValue = inputDataUpper.get(sapField.getName());

            // 请求数据字段为空，不赋值
            if (inputValue == null) {
                continue;
            }

            if (FuseConstants.SAP_DATA_TYPE_VALUE.equals(sapField.getIsTable())) {
                //普通字段类型
                if (!(inputValue instanceof String)) {
                    throw new RequestParamException(FuseConstants.ERROR_MSG + sapField.getName());
                }
                //处理特殊类型
                Object value = parse(sapField, inputValue);
                requestEndpoint.put(sapField.getName(), value);
            } else if (FuseConstants.SAP_DATA_TYPE_TABLE.equals(sapField.getIsTable())) {
                //集合类型
                if (inputValue instanceof JSONArray) {
                    Table<Structure> table = (Table<Structure>) requestEndpoint.get(sapField.getName());
                    List<SapField> fields = sapField.getTable();
                    JSONArray inputArr = (JSONArray) inputValue;
                    // 遍历输入数据数组
                    for (Object item : inputArr) {
                        Map<String, Object> inputRow = mapKeyUpper((JSONObject) item);
                        Structure add = table.add();
                        // 遍历所有字段赋值
                        for (SapField field : fields) {
                            Object inputValueItem = inputRow.get(field.getName());
                            if (!(inputValueItem instanceof String)) {
                                throw new RequestParamException(FuseConstants.ERROR_MSG + field.getName());
                            }

                            inputValueItem = parse(field, inputValueItem);
                            add.put(field.getName(), inputValueItem);
                        }
                    }
                } else {
                    log.warn("input data is not table:" + inputValue);
                }
            } else if (FuseConstants.SAP_DATA_TYPE_OBJECT.equals(sapField.getIsTable())) {
                //嵌套类型，重复执行上面得逻辑，只需要支持2层嵌套
                Map<String, Object> inputObj = mapKeyUpper((JSONObject) inputValue);
                Structure structure = (Structure) requestEndpoint.get(sapField.getName());
                List<SapField> fields = sapField.getTable();
                for (SapField field : fields) {
                    Object inputValueItem = inputObj.get(field.getName());
                    if (!(inputValueItem instanceof String)) {
                        throw new Exception(FuseConstants.ERROR_MSG + field.getName());
                    }

                    inputValueItem = parse(field, inputValueItem);
                    structure.put(field.getName(), inputValueItem);
                }
            } else {
                log.warn("type error:" + inputValue);
            }
        }
    }

    /**
     * 将输入数据key转换为大写
     */
    public Map<String, Object> mapKeyUpper(JSONObject map) {
        HashMap<String, Object> mapKeyUpper = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            mapKeyUpper.put(entry.getKey().toUpperCase(Locale.ROOT), entry.getValue());
        }
        return mapKeyUpper;
    }

    /**
     * 获取sap数据结构
     */
    private List<SapField> getSapStructure(String fun, SapSynchronousRfcDestinationEndpoint endpoint, Structure requestEndpoint) throws JCoException {
        JCoRepository repository = JCoDestinationManager.getDestination((endpoint.getDestinationName())).getRepository();
        String nsURI = "http://sap.fusesource.org/rfc/" + repository.getName() + "/" + SapManage.convertSAPNamespaceToXMLName(fun);
        //获取RFC在组件中缓存得元数据信息，其中包括结构、描述等
        EPackage ePackage = RfcUtil.getEPackage(repository, nsURI);
        EClassifier classifier = ePackage.getEClassifier("Request");
        //该对象中包括请求参数结构得类型描述信息等
        EList<EStructuralFeature> eStructuralFeatures = ((EClassImpl) classifier).getEStructuralFeatures();
        List<SapField> listFields = new ArrayList<>();
        for (EStructuralFeature eStructuralFeature : eStructuralFeatures) {
            //得到参数在SAP中定义得类型
            Object sapValue = requestEndpoint.get(eStructuralFeature.getName());
            //获取参数其他自定义得描述信息
            SapField sapField = getSapField(eStructuralFeature, sapValue);
            listFields.add(sapField);

            if (sapValue instanceof Table) {
                //参数是table，获取table下得其他字段
                sapField.setTable(getTable((Table<Structure>) sapValue));
            } else if (sapValue instanceof StructureImpl) {
                //参数是structure，按照上面得逻辑再获取一次最终得到table及下面得字段
                EList<EStructuralFeature> structuralList = ((StructureImpl) sapValue).eClass().getEStructuralFeatures();
                List<SapField> structureFields = new ArrayList<>();
                for (EStructuralFeature structuralFeature : structuralList) {
                    SapField structureField = getSapField(structuralFeature, ((Structure) sapValue).get(structuralFeature.getName()));
                    structureFields.add(structureField);
                }
                sapField.setTable(structureFields);
            }
        }

        return listFields;
    }

    /**
     * 获取table下得字段得到自定义得信息
     *
     * @param eStructuralFeature
     * @param sapValue
     * @return
     */
    private SapField getSapField(EStructuralFeature eStructuralFeature, Object sapValue) {
        SapField sapField = new SapField();
        String fieldType = eStructuralFeature.getEType().getInstanceTypeName();
        sapField.setName(eStructuralFeature.getName());
        String isTable = getValueType(sapValue);
        if (fieldType == null) {
            fieldType = isTable;
        }
        sapField.setType(fieldType);

        sapField.setIsTable(isTable);
        if ("java.util.Date".equals(fieldType)) {
            try {
                sapField.setLength(Integer.valueOf(eStructuralFeature.getEAnnotation("http://sap.fusesource.org/rfc").getDetails().get("length")));
            } catch (Exception e) {
                sapField.setLength(8);
            }
        }
        return sapField;
    }

    /**
     * 获取value的类型
     */
    public String getValueType(Object sapValue) {
        // 值类型
        String isTable = "";
        if (sapValue instanceof Table) {
            isTable = FuseConstants.SAP_DATA_TYPE_TABLE;
        } else if (sapValue instanceof Structure) {
            isTable = FuseConstants.SAP_DATA_TYPE_OBJECT;
        } else {
            isTable = FuseConstants.SAP_DATA_TYPE_VALUE;
        }
        return isTable;
    }

    /**
     * 数据结构为List集合结构，完成结构解析
     */
    private List<SapField> getTable(Table<Structure> sapValue) {
        Table<Structure> table = sapValue;
        EStructuralFeature feature = table.eClass().getEStructuralFeature("row");
        EClassImpl eClass = (EClassImpl) ((EReference) feature).getEReferenceType();
        EList<EStructuralFeature> features = eClass.getEStructuralFeatures();
        List<SapField> tableFields = new ArrayList<>();
        for (EStructuralFeature structuralFeature : features) {
            SapField sap = getSapField(structuralFeature, "");
            tableFields.add(sap);
        }
        return tableFields;
    }


    /**
     * sap数据类型转换为java类型
     */
    public Object parse(SapField sapField, Object inputValue) throws ParseException {
        String fieldTypeStr = sapField.getType();
        if (fieldTypeStr == null) {
            return inputValue;
        }
        switch (fieldTypeStr) {
            case "java.util.Date":
                inputValue = parseDate((String) inputValue, sapField.getLength());
                break;
            case "java.math.BigDecimal":
                inputValue = convertToBigDecimal(inputValue);
                break;
            default:
                break;
        }
        return inputValue;
    }

    public BigDecimal convertToBigDecimal(Object object) {
        if (object instanceof BigDecimal) {
            return (BigDecimal) object;
        } else if (object instanceof Integer) {
            return BigDecimal.valueOf((Integer) object);
        } else if (object instanceof Long) {
            return BigDecimal.valueOf((Long) object);
        } else if (object instanceof Double) {
            return BigDecimal.valueOf((Double) object);
        } else if (object instanceof String) {
            try {
                return  new BigDecimal((String) object);
            } catch (NumberFormatException e) {
                throw new RequestParamException(e.getMessage());
            }
        } else {
            throw new RequestParamException("Unsupported object type for conversion to BigDecimal");
        }
    }

    public Date parseDate(String dateString, Integer length) throws ParseException {
        DateFormat dateFormat = null;

        switch (length) {
            case 10:
                dateFormat = new SimpleDateFormat("yyyy-MM-dd HH");
                break;
            case 12:
                dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                break;
            case 14:
                dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                break;
            case 17:
                dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                break;
            default:
                dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                break;
        }
        Date date = null;
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            log.warn("parseDate error:" + dateString);
            throw new RequestParamException(e.getMessage());
        }
        return date;
    }
}
