package com.bookstore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_book")
public class Book extends BaseEntity {
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDate publishDate;
    private String coverUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer sales;
    private BigDecimal rating;
    private String description;
    private Long categoryId;
    private Integer status;

    // ===== 新增：商家相关字段 =====
    @JsonSerialize(using = ToStringSerializer.class)
    private Long shopId;              // 所属店铺ID
    private Integer isAdminCreated;   // 是否管理员创建 0否 1是
}