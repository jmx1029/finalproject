package com.bookstore.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShopUpdateDTO {

    @Size(max = 64, message = "店铺名称长度不能超过 64 个字符")
    private String name;

    private String logo;

    @Size(max = 500, message = "店铺描述长度不能超过 500 个字符")
    private String description;

    @Size(max = 32, message = "联系人姓名长度不能超过 32 个字符")
    private String contactPerson;

    @Size(max = 20, message = "联系电话长度不能超过 20 个字符")
    private String contactPhone;

    @Size(max = 200, message = "店铺地址长度不能超过 200 个字符")
    private String address;

    @Size(max = 500, message = "公告长度不能超过 500 个字符")
    private String announcement;
}
