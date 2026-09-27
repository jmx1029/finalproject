package com.bookstore.vo;

import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailVO extends Order {
    private List<OrderItem> orderItems;
    private String shopName;
}