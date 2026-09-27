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
@TableName("tb_session")
public class Session implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long user1Id;
    private Long user2Id;

    private String lastMessage;
    private LocalDateTime lastMessageTime;

    private Integer unreadCountUser1;
    private Integer unreadCountUser2;

    private Integer user1Deleted;
    private Integer user2Deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}