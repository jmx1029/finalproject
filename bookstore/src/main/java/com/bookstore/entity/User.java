package com.bookstore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String signature;   // ← 新增：个性签名
    private Integer role;
    private Integer status;

    // =====商家相关字段 =====
    @JsonSerialize(using = ToStringSerializer.class)
    private Long shopId;          // 关联的店铺ID
    private Integer isShopOwner = 0;  // 是否是商家 0否 1是
    private Integer shopStatus = 0;   // 商家状态 0未申请 1审核中 2已通过 3已拒绝
}