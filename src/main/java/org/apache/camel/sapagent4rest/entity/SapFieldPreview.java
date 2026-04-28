package org.apache.camel.sapagent4rest.entity;

import lombok.Data;

@Data
public class SapFieldPreview extends PreviewSapField {
    private String subName;
    private String lengthStr;
}
