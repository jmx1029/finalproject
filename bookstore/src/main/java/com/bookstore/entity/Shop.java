package com.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_shop")
public class Shop implements Serializable {
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long userId;
    private String name;
    private String logo;
    private String description;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private Integer status;      // 0禁用 1正常
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    private String announcement;  // 店铺公告
    private BigDecimal avgRating;    // 店铺平均评分（10分制，保留一位小数）
    private Integer ratingCount;     // 店铺评分人数
}