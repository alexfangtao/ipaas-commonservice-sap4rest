package org.apache.camel.sapagent4rest.entity;


import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;


@Data
public class LogEntity {
    /**
     * 接口编号
     */
    @JSONField(ordinal = 1)
    private String svcNo;

    /**
     * 链路ID
     */
    @JSONField(ordinal = 2)
    private String uuId;
    /**
     * 业务id
     */
    @JSONField(ordinal = 3)
    private String traceId;
    /**
     * 8100、8200、8300、8400、9100
     */
    @JSONField(ordinal = 4)
    private String code;

    @JSONField(ordinal = 5)
    private String fromApp;

    @JSONField(ordinal = 6)
    private String toApp;
    /**
     * 请求参数内容
     */
    @JSONField(ordinal = 7)
    private String body;
    /**
     * 日志生成时间
     */
    @JSONField(ordinal = 8)
    private Long msgTs;
}
