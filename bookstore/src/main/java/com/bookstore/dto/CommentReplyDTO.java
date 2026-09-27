package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论回复 DTO
 * <p>
 * 阶段四 T4：替代 Controller 层 Map 手写解析，统一走 @Valid 校验
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommentReplyDTO {

    /** 父评论ID（必须是顶级评论） */
    @NotNull(message = "父评论ID不能为空")
    private Long parentId;

    /** 被回复的用户ID（可选；为 null 时默认为父评论作者） */
    private Long replyToUserId;

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 300, message = "回复内容不能超过300字")
    private String content;
}
