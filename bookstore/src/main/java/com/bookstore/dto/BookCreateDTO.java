package com.bookstore.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商家新建图书 DTO
 * 仅暴露业务字段，敏感字段（shopId/sales/rating/status/isAdminCreated/isDeleted）由 Service 层手动设置
 */
@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class BookCreateDTO {

    @NotBlank(message = "图书标题不能为空")
    @Size(max = 200, message = "图书标题长度不能超过 200 个字符")
    private String title;

    @Size(max = 100, message = "作者长度不能超过 100 个字符")
    private String author;

    @Size(max = 50, message = "ISBN 长度不能超过 50 个字符")
    private String isbn;

    @Size(max = 100, message = "出版社长度不能超过 100 个字符")
    private String publisher;

    private LocalDate publishDate;

    @Size(max = 500, message = "封面URL长度不能超过 500 个字符")
    private String coverUrl;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "原价不能小于 0")
    private BigDecimal originalPrice;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于 0")
    private Integer stock;

    @Size(max = 2000, message = "描述长度不能超过 2000 个字符")
    private String description;

    private Long categoryId;
}
