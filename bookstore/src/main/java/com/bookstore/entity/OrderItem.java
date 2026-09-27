package com.bookstore.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_order_item")
public class OrderItem implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)   // ← 添加这行
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)   // ← 添加这行
    private Long orderId;

    @JsonSerialize(using = ToStringSerializer.class)   // ← 添加这行
    private Long bookId;
    private String bookTitle;    // 书名快照
    private String bookCover;    // 封面快照
    private BigDecimal price;    // 单价快照
    private Integer quantity;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}