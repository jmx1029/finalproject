package com.bookstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderSubmitDTO {

    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 32, message = "收货人姓名长度不能超过 32 个字符")
    private String receiverName;

    @NotBlank(message = "收货人电话不能为空")
    @Size(max = 20, message = "收货人电话长度不能超过 20 个字符")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    @Size(max = 200, message = "收货地址长度不能超过 200 个字符")
    private String receiverAddress;
}
