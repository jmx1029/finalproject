package com.bookstore.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long bookId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String content;
    private Integer rating;
    private Integer likeCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createTime;

    // 关联图书信息
    private String bookTitle;
    private String bookCover;

    // 商家回复
    private String replyContent;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime replyTime;

    // 用户回复体系（抖音风格）
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long replyToUserId;
    private String replyToUsername;   // 被回复人的昵称（抖音里"回复 某用户"）
    private Integer replyCount;       // 该评论的直接回复数
    private List<CommentVO> replies;  // 最近 N 条回复（嵌套只到这一层）

    // 权限
    private Boolean canDelete;
    // 当前登录用户是否点过赞（前端用来判断爱心红色/灰色）
    private Boolean liked;
}
