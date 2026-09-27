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
@TableName("tb_message")
public class Message implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiverId;

    private String content;

    private Integer messageType;  // 0文本 1订单卡片 2图片

    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;        // 关联订单ID

    private Integer isRead;
    private LocalDateTime readTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}