package com.bookstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.entity.Session;
import com.bookstore.vo.SessionVO;

import java.util.List;

public interface SessionService {

    /**
     * 获取或创建会话（两个用户之间只有一个会话）
     */
    Session getOrCreateSession(Long user1Id, Long user2Id);

    /**
     * 获取用户的会话列表（按最后消息时间降序）
     */
    IPage<SessionVO> getUserSessions(Long userId, Integer page, Integer size);

    /**
     * 更新会话最后消息
     */
    void updateLastMessage(Long sessionId, String content, Long senderId);

    /**
     * 增加对方未读计数
     */
    void incrementUnreadCount(Long sessionId, Long receiverId);

    /**
     * 标记会话已读（清零当前用户的未读计数）
     */
    void markSessionRead(Long sessionId, Long userId);

    /**
     * 获取会话中的对方用户ID
     */
    Long getOtherUserId(Long sessionId, Long currentUserId);

    /**
     * 标记当前用户对该会话"软删除"（对方还能看到）
     * 双方都删了 → 内部会触发 isDeleted=1
     */
    void markUserDeleted(Long sessionId, Long currentUserId);
}