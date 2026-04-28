package org.apache.camel.sapagent4rest.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "camel.custom")
public class ESBProperties {
    private String restful;
    private String delCache;
}
