package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 图书顶级评论 DTO
 * <p>
 * 阶段四 T4：替代 Controller 层 Map 手写解析，统一走 @Valid 校验
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentCreateDTO {

    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500字")
    private String content;

    /** 顶级评论必须有评分（0-10 分制），Service 层会触发 recalculateBookRating */
    @NotNull(message = "评分不能为空")
    @Min(value = 0, message = "评分不能低于0")
    @Max(value = 10, message = "评分不能超过10")
    private Integer rating;
}
