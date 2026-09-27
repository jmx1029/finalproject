package com.bookstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.dto.SendMessageDTO;
import com.bookstore.entity.Message;
import com.bookstore.vo.MessageVO;

public interface MessageService {

    /**
     * 发送消息（支持文本、订单卡片、图片）
     */
    Message sendMessage(Long senderId, SendMessageDTO dto);

    /**
     * 获取会话的消息列表（分页，按时间升序）
     */
    IPage<MessageVO> getSessionMessages(Long sessionId, Long currentUserId, Integer page, Integer size);

    /**
     * 获取消息详情
     */
    MessageVO getMessageDetail(Long messageId, Long currentUserId);

    /**
     * 标记会话所有消息为已读
     */
    void markAllAsRead(Long sessionId, Long userId);

    /**
     * 获取用户未读消息总数
     */
    Integer getUnreadCount(Long userId);
}