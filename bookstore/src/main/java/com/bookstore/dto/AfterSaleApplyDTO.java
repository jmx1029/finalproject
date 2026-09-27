package com.bookstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AfterSaleApplyDTO {

    /**
     * 订单ID（使用 String 防止前端 Long 精度丢失）
     * 例如：2095058206442770433
     */
    @NotBlank(message = "订单ID不能为空")
    private String orderId;

    /**
     * 售后类型：1-仅退款，2-退货退款
     */
    @NotNull(message = "售后类型不能为空")
    @Min(value = 1, message = "售后类型值无效")
    private Integer type;

    /**
     * 申请原因（如：质量问题、发错货等）
     */
    @NotBlank(message = "申请原因不能为空")
    @Size(max = 200, message = "申请原因长度不能超过 200 个字符")
    private String reason;

    /**
     * 详细描述
     */
    @Size(max = 1000, message = "详细描述长度不能超过 1000 个字符")
    private String description;

    /**
     * 申请退款金额
     */
    private BigDecimal amount;
}
