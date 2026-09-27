package com.bookstore.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CategoryVO implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;
    private List<CategoryVO> children;
}