package com.bookstore.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SessionVO implements Serializable {
    private Long sessionId;
    private Long targetUserId;        // 对方用户ID
    private String targetUsername;    // 对方用户名
    private String targetNickname;    // 对方昵称
    private String targetAvatar;      // 对方头像
    private String targetRole;        // 对方角色：user/shop/admin
    private String lastMessage;       // 最后一条消息摘要
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;      // 当前用户的未读数量
}