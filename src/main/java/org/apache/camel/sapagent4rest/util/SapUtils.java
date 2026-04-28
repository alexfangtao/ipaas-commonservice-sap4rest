package org.apache.camel.sapagent4rest.util;

import com.sap.conn.idoc.jco.JCoIDocServer;
import com.sap.conn.jco.*;
import org.apache.camel.sapagent4rest.FuseConstants;
import org.apache.camel.sapagent4rest.entity.PreviewSapField;
import org.apache.camel.sapagent4rest.entity.SAPDataTypes;
import org.fusesource.camel.component.sap.ServerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SapUtils {

    public static List<PreviewSapField> getResponseParameter(String serverName, String functionName, Boolean isServer) throws Exception {
        JCoFunction function = getjCoFunction(serverName, functionName, isServer);
        JCoParameterList importParaList = function.getExportParameterList();
        JCoParameterList tableParaList = function.getTableParameterList();

        return genParameterListJavaSource(importParaList, tableParaList);
    }

    public static List<PreviewSapField> getRequestParameter(String serverName, String functionName, Boolean isServer) throws Exception {
        JCoFunction function = getjCoFunction(serverName, functionName, isServer);
        if (function == null) {
            throw new RuntimeException("RFC does not exist!");
        }
        JCoParameterList importParaList = function.getImportParameterList();
        JCoParameterList tableParaList = function.getTableParameterList();

        return genParameterListJavaSource(importParaList, tableParaList);
    }


    private static JCoFunction getjCoFunction(String serverName, String functionName, Boolean isServer) throws Exception {
        JCoRepository repository = null;
        if (isServer) {
            JCoIDocServer server = ServerManager.INSTANCE.getServer(serverName);
            repository = server.getRepository();
        } else {
            JCoDestination destination = JCoDestinationManager.getDestination(serverName);
            repository = destination.getRepository();
        }

        return repository.getFunction(functionName);
    }

    public static List<PreviewSapField> genParameterListJavaSource(JCoParameterList paraList, JCoParameterList tableParaList) {
        List<PreviewSapField> fList = new ArrayList<>();
        Map<String, String> sapTypeMappings = SAPDataTypes.getTypeMappings();
        if (paraList != null) {
            int index =0;
            for (JCoField rec : paraList) {
                index++;
                String fName = rec.getName();
                String fType = rec.getTypeAsString();
                String fDesc = rec.getDescription();
                int length = rec.getLength();
                PreviewSapField sapField = new PreviewSapField(fName, fType, fDesc,length);
                sapField.setOrder(index);
                sapField.setDecimal(rec.getDecimals());
                if ("TABLE".equals(fType)) {
                    sapField.setIsTable(FuseConstants.SAP_DATA_TYPE_TABLE);
                    fList.add(sapField);
                    sapField.setTable(genTableJavaSource(rec.getRecordMetaData()));
                    continue;
                } else if ("STRUCTURE".equals(fType)) {
                    sapField.setIsTable(FuseConstants.SAP_DATA_TYPE_OBJECT);
                    fList.add(sapField);
                    sapField.setTable(genTableJavaSource(rec.getRecordMetaData()));
                    continue;
                } else {
                    String javaTypeStr = sapTypeMappings.get(fType);
                    sapField.setIsTable(FuseConstants.SAP_DATA_TYPE_VALUE);
                    if (javaTypeStr != null) {
                        sapField.setType(javaTypeStr);
                        fList.add(sapField);
                    } else {
                        fList.add(sapField);
                    }
                }
            }
        }

        if (tableParaList != null) {
            int index =0;
            for (JCoField recTable : tableParaList) {
                index++;
                String fName = recTable.getName();
                String fType = recTable.getTypeAsString();
                String fDesc = recTable.getDescription();
                int length = recTable.getLength();
                PreviewSapField sapField = new PreviewSapField(fName, fType, fDesc,length);
                sapField.setOrder(index);
                sapField.setDecimal(recTable.getDecimals());
                if ("TABLE".equals(fType)) {
                    sapField.setIsTable(FuseConstants.SAP_DATA_TYPE_TABLE);
                    fList.add(sapField);
                    sapField.setTable(genTableJavaSource(recTable.getRecordMetaData()));
                    continue;
                }
                if ("STRUCTURE".equals(fType)) {
                    sapField.setIsTable(FuseConstants.SAP_DATA_TYPE_OBJECT);
                    fList.add(sapField);
                    sapField.setTable(genTableJavaSource(recTable.getRecordMetaData()));
                    continue;
                }
            }
        }

        return fList;
    }

    private static List<PreviewSapField> genTableJavaSource(JCoRecordMetaData tableRecordMetaData) {
        List<PreviewSapField> fList = new ArrayList<>();
        Map<String, String> sapTypeMappings = SAPDataTypes.getTypeMappings();
        for (int i = 0; i < tableRecordMetaData.getFieldCount(); i++) {
            String fDesc = tableRecordMetaData.getDescription(i);
            String fType = tableRecordMetaData.getTypeAsString(i);
            String fName = tableRecordMetaData.getName(i);
            int length = tableRecordMetaData.getLength(i);
            PreviewSapField sapField = new PreviewSapField(fName, fType, fDesc,length);
            sapField.setOrder(i);
            sapField.setDecimal(tableRecordMetaData.getDecimals(i));
            if ("TABLE".equals(fType)) {
                sapField.setIsTable(FuseConstants.SAP_DATA_TYPE_TABLE);
                fList.add(sapField);
                sapField.setTable(genTableJavaSource(tableRecordMetaData.getRecordMetaData(i)));
            } else if ("STRUCTURE".equals(fType)) {
                sapField.setIsTable(FuseConstants.SAP_DATA_TYPE_OBJECT);
                fList.add(sapField);
                sapField.setTable(genTableJavaSource(tableRecordMetaData.getRecordMetaData(i)));
            } else {
                sapField.setIsTable(FuseConstants.SAP_DATA_TYPE_VALUE);
                String javaTypeStr = sapTypeMappings.get(fType);
                if (javaTypeStr != null) {
                    sapField.setType(javaTypeStr);
                    fList.add(sapField);
                } else {
                    fList.add(sapField);
                }
            }
        }

        return fList;
    }
}
