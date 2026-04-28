package org.apache.camel.sapagent4rest.entity;

import lombok.Data;

import java.util.List;

@Data
public class PreviewSapField {
    private Integer order;
    private String name;

    private String type;

    private String isTable;

    private List<PreviewSapField> table;

    private String desc;
    private Integer length;
    private Integer decimal;


    public PreviewSapField(){}

    public PreviewSapField(String name, String type, String desc, Integer length) {
        this.name = name;
        this.type = type;
        this.desc = desc;
        this.length=length;
    }

}
