package com.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_shop_application")
public class ShopApplication implements Serializable {
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long userId;
    private String shopName;
    private String description;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String idCard;
    private String idCardFront;
    private String idCardBack;
    private String businessLicense;
    private Integer status;      // 0待审核 1已通过 2已拒绝
    private String reviewRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}