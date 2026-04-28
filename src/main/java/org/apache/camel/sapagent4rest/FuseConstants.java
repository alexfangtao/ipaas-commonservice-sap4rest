package org.apache.camel.sapagent4rest;

public class FuseConstants {

	public static final String DESTINATION ="DESTINATION";
	public static final String RFC = "RFC";

	public static final String FROM_SYS = "X-FROM-SYS-ID";//源系统ID， 例如： EPMS

	public static final String HEADER_FROM = "client_id";
	public static final String TO_SYS = "X-TO-SYS-ID";//目标系统ID， 例如：PPOMS

	public static final String SAP_DATA_TYPE_VALUE = "value";
	public static final String SAP_DATA_TYPE_TABLE = "table";
	public static final String SAP_DATA_TYPE_OBJECT = "object";

	public static final String SVC_NO = "SVCNO";

	public static final String ERROR_MSG = "入参字段缺失或输入不合法";

	public static final String FUNCTION = "FUNCTION";
    public static final String DES_NAME = "desName";
	public static final String RFC_LOWERCASE = "rfc";
	public static final String ID = "id";
	public static final String STRING_AT = "@";
	public static final String STRING_COLON = ":";
	public static final String TRACE_ID = "trace_id";
}
