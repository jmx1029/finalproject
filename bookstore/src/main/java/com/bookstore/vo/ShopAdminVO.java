package com.bookstore.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ShopAdminVO implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private Long userId;
    private String ownerUsername;
    private String ownerNickname;

    private String name;
    private String logo;
    private String description;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String announcement;

    private Integer status;          // 0停业整顿 1正常营业
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}