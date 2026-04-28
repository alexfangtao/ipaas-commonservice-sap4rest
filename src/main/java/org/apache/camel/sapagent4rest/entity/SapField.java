package org.apache.camel.sapagent4rest.entity;

import lombok.Data;

import java.util.List;

@Data
public class SapField {
    private String name;

    private String type;

    private String isTable;

    private List<SapField> table;

    private String desc;

    private Integer length;
    private Integer decimal;

    public SapField(){}

    public SapField(String name, String type, String desc, String isTable) {
        this.name = name;
        this.type = type;
        this.desc = desc;
        this.isTable = isTable;
    }

}
