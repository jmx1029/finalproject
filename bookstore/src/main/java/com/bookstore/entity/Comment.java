package com.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_comment")
public class Comment implements Serializable {
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long bookId;
    private Long userId;
    private String content;
    private Integer rating;
    private Integer likeCount;
    private String replyContent;   // 商家回复内容
    private LocalDateTime replyTime; // 商家回复时间

    // ===== 用户回复体系（抖音风格） =====
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;          // 父评论ID（NULL=顶级评论）
    @JsonSerialize(using = ToStringSerializer.class)
    private Long replyToUserId;     // 被回复的用户ID

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;

}