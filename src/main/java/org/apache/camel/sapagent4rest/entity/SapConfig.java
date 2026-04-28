package org.apache.camel.sapagent4rest.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SapConfig {
    private String configId;

    private String enabled; //是否开启，默认不开启【0关闭，1开启】

    private String svcNo;//svcNo号

    private String destination;

    private String rfcName;

    private String refreshMark;

    private Date crtTs;//创建时间

    private Date lastUpdTs;//最后更新时间

    /**
     * 控制缓存的失效时间，时间戳格式
     */
    private Long failureTime;
}
