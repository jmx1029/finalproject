package com.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_order")
public class Order implements Serializable {

    // ==================== 订单状态常量 ====================
    public static final int STATUS_WAIT_PAY = 0;      // 待付款
    public static final int STATUS_PAID = 1;          // 已支付
    public static final int STATUS_SHIPPED = 2;       // 已发货
    public static final int STATUS_COMPLETED = 3;     // 已完成
    public static final int STATUS_CANCELLED = 4;     // 已取消
    public static final int STATUS_AFTER_SALE = 5;    // 售后中
    public static final int STATUS_REFUNDED = 6;      // 已退款
    public static final int STATUS_CLOSED = 7;        // 已关闭（售后驳回）

    // ==================== 数据库字段 ====================
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private Long userId;

    private BigDecimal totalAmount;

    private Integer status;  // 使用上面的常量

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime finishTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;

    private Long shopId;  // 所属店铺ID
}