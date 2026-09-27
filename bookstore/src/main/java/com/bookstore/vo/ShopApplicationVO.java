package com.bookstore.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ShopApplicationVO implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String shopName;
    private String description;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String idCard;
    private String idCardFront;
    private String idCardBack;
    private String businessLicense;
    private Integer status;
    private String statusText;
    private String reviewRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}