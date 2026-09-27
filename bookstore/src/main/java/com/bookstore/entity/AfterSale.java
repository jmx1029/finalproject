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
@TableName("tb_after_sale")
public class AfterSale implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;
    private Long userId;
    private Long shopId;
    private Integer type;       // 1仅退款 2退货退款
    private String reason;
    private String description;
    private BigDecimal amount;
    private Integer status;     // 0待商家审核 1商家通过 2商家驳回 3用户已退货(待仲裁) 4仲裁完成 5已关闭
    private LocalDateTime applyTime;
    private LocalDateTime reviewTime;
    private LocalDateTime completeTime;
    private String shopReviewRemark;
    private String adminReviewRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}