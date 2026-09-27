package com.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tb_message_attachment")
public class MessageAttachment implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private LocalDateTime createTime;
}