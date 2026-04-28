package org.apache.camel.sapagent4rest.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class SAPDataTypes {
    private static final Map<String, String> TYPE_MAPPINGS;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("CHAR", "String");
        map.put("BCD", "BigDecimal");
        map.put("NUM", "Number");
        map.put("DATE", "java.util.Date");
        map.put("TIME", "java.util.Date");
        TYPE_MAPPINGS = Collections.unmodifiableMap(map);
    }

    private SAPDataTypes() {
        // 工具类禁止实例化
    }

    public static Map<String, String> getTypeMappings() {
        return TYPE_MAPPINGS;
    }
}
