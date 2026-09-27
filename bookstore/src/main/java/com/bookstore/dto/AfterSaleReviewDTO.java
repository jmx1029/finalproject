package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AfterSaleReviewDTO {

    /**
     * 审核状态：1-商家通过，2-商家驳回
     */
    @NotNull(message = "审核状态不能为空")
    @Min(value = 1, message = "审核状态值无效")
    private Integer status;

    /**
     * 审核备注（驳回原因 / 通过说明）
     */
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;
}
