package com.bookstore.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

@Data
public class ShopVO implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long userId;
    private String name;
    private String logo;
    private String description;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String announcement;   // ← 新增
    private Integer status;
    private Integer isShopOwner;
    private Integer shopStatus;
    private Double avgRating;       // 店铺平均评分
    private Integer ratingCount;    // 店铺评分人数
}