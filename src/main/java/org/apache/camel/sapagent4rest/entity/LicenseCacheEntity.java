package org.apache.camel.sapagent4rest.entity;

import lombok.Data;

@Data
public class LicenseCacheEntity {

    private Long licenseTime;

    /**
     * 控制缓存的失效时间，时间戳格式
     */
    private Long failureTime;
}
