package com.bookstore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@TableName("tb_category")
public class Category extends BaseEntity {
    private String name;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    private Integer sort;
}