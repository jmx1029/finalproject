package com.bookstore.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CartItemVO implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bookId;
    private String title;
    private String coverUrl;
    private BigDecimal price;
    private Integer quantity;
    private Integer stock;
    private Boolean selected;
}