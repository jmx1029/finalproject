package com.bookstore.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageVO implements Serializable {
    private Long id;
    private Long sessionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    private String senderName;
    private String senderAvatar;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiverId;

    private String content;
    private Integer messageType;      // 0文本 1订单卡片 2图片

    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;             // 关联订单ID

    private Boolean isMine;           // 是否当前用户发送
    private Integer isRead;
    private LocalDateTime createTime;

    // ===== 新增：附件列表 =====
    private List<MessageAttachmentVO> attachments;  // 附件列表
}

