package com.bookstore.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;

@Data
public class LoginVO implements Serializable {
    private String token;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer role;

    @JsonProperty("isShopOwner")
    private Integer isShopOwner;

    private Long shopId;
    private Integer shopStatus;

}