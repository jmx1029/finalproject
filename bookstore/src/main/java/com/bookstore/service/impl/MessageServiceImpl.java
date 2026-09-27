package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.common.ErrorCode;
import com.bookstore.dto.SendMessageDTO;
import com.bookstore.entity.*;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.MessageAttachmentMapper;
import com.bookstore.mapper.MessageMapper;
import com.bookstore.service.MessageService;
import com.bookstore.service.OrderService;
import com.bookstore.service.SessionService;
import com.bookstore.service.ShopService;
import com.bookstore.service.UserService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.MessageAttachmentVO;
import com.bookstore.vo.MessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final SessionService sessionService;
    private final UserService userService;
    private final OrderService orderService;
    private final MessageAttachmentMapper attachmentMapper;
    private final ShopService shopService;

    @Autowired
    public MessageServiceImpl(SessionService sessionService,
                              UserService userService,
                              OrderService orderService,
                              MessageAttachmentMapper attachmentMapper,
                              ShopService shopService) {
        this.sessionService = sessionService;
        this.userService = userService;
        this.orderService = orderService;
        this.attachmentMapper = attachmentMapper;
        this.shopService = shopService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message sendMessage(Long senderId, SendMessageDTO dto) {
        // 0. 条件校验：只有文本消息(messageType=0)才要求 content 非空
        int msgType = dto.getMessageType() != null ? dto.getMessageType() : 0;
        if (msgType == 0 && (dto.getContent() == null || dto.getContent().trim().isEmpty())) {
            throw new BusinessException(ErrorCode.MESSAGE_CONTENT_REQUIRED);
        }
        // 图片消息必须有图片 URL
        if (msgType == 2 && (dto.getImageUrls() == null || dto.getImageUrls().isEmpty())) {
            throw new BusinessException(ErrorCode.MESSAGE_CONTENT_REQUIRED, "图片消息缺少图片");
        }
        // 订单卡片必须有 orderId
        if (msgType == 1 && (dto.getOrderId() == null || dto.getOrderId().isEmpty())) {
            throw new BusinessException(ErrorCode.MESSAGE_CONTENT_REQUIRED, "订单卡片缺少订单ID");
        }

        // ===== 订单卡片权限校验前，先解析接收方（订单卡片校验需要用到 receiverId）=====
        Long receiverIdLong;
        try {
            receiverIdLong = Long.parseLong(dto.getReceiverId());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.RECEIVER_ID_FORMAT_ERROR);
        }

        // ===== 订单卡片权限校验：当前发送方必须有权发送该订单 =====
        if (msgType == 1) {
            Long orderId;
            try {
                orderId = Long.parseLong(dto.getOrderId());
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.ORDER_ID_FORMAT_ERROR);
            }
            Order order = orderService.getById(orderId);
            if (order == null) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }

            Integer role = UserContext.getRole();
            boolean allowed = false;
            if (role != null && role == 1) {
                // 管理员可以发任何订单
                allowed = true;
            } else if (order.getUserId() != null && order.getUserId().equals(senderId)) {
                // 买家只能发自己下的订单
                allowed = true;
            } else {
                // 商家：查 sender 是否是商家 + 该订单是否属于其店铺
                try {
                    Long myShopId = shopService.getShopIdByUserId(senderId);
                    if (myShopId != null && myShopId.equals(order.getShopId())) {
                        allowed = true;
                    }
                } catch (Exception ignored) {}
            }

            if (!allowed) {
                throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION, "无权发送此订单卡片");
            }

            // === 阶段三 T6：兜底校验接收方是否与订单有合理关系 ===
            // 防止"买家把 A 店订单发给 B 店商家"这种越权泄露
            if (role == null || role != 1) {
                // 管理员可以发给任何人，跳过；非管理员必须校验
                boolean receiverAllowed = false;
                // 先判断接收方是啥角色
                User receiver = userService.getById(receiverIdLong);
                if (receiver != null && receiver.getRole() != null && receiver.getRole() == 1) {
                    receiverAllowed = true;   // 管理员接收方通吃
                } else if (order.getUserId() != null && order.getUserId().equals(receiverIdLong)) {
                    receiverAllowed = true;   // 接收方是订单买家（商家发给买家）
                } else {
                    // 接收方是商家：订单 shopId 必须属于接收方的店铺
                    try {
                        Long receiverShopId = shopService.getShopIdByUserId(receiverIdLong);
                        if (receiverShopId != null && receiverShopId.equals(order.getShopId())) {
                            receiverAllowed = true;
                        }
                    } catch (Exception ignored) {}
                }
                if (!receiverAllowed) {
                    throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION, "该订单卡片不能发送给此接收方");
                }
            }
        }

        // 2. 获取或创建会话（receiverIdLong 已在订单卡片校验前解析）
        Session session = sessionService.getOrCreateSession(senderId, receiverIdLong);

        // 3. 构建消息
        Message message = new Message();
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverIdLong);
        message.setContent(dto.getContent());
        message.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : 0);
        // ===== 关键修复：orderId 转为 Long =====
        if (dto.getOrderId() != null && !dto.getOrderId().isEmpty()) {
            try {
                message.setOrderId(Long.parseLong(dto.getOrderId()));
            } catch (NumberFormatException e) {
                log.warn("订单ID格式错误: {}", dto.getOrderId());
            }
        }
        message.setIsRead(0);
        this.save(message);

        // 4. 处理附件（图片）
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            for (String imageUrl : dto.getImageUrls()) {
                MessageAttachment attachment = new MessageAttachment();
                attachment.setMessageId(message.getId());
                attachment.setFileUrl(imageUrl);
                attachment.setFileName(imageUrl.substring(imageUrl.lastIndexOf("/") + 1));
                attachmentMapper.insert(attachment);
            }
        }

        // 5. 更新会话最后消息
        String summary = dto.getContent();
        if (dto.getMessageType() != null && dto.getMessageType() == 1) {
            summary = "[订单卡片]";
        } else if (dto.getMessageType() != null && dto.getMessageType() == 2) {
            summary = "[图片]";
        }
        sessionService.updateLastMessage(session.getId(), summary, senderId);

        // 6. 增加接收方未读计数
        sessionService.incrementUnreadCount(session.getId(), receiverIdLong);

        return message;
    }

    @Override
    public IPage<MessageVO> getSessionMessages(Long sessionId, Long currentUserId, Integer page, Integer size) {
        // 验证会话是否存在且用户有权限访问
        Long otherUserId = sessionService.getOtherUserId(sessionId, currentUserId);
        if (otherUserId == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sessionId)
                .eq(Message::getIsDeleted, 0)
                .orderByAsc(Message::getCreateTime);

        IPage<Message> messagePage = this.page(new Page<>(page, size), wrapper);
        List<Message> records = messagePage.getRecords();

        // ===== N+1 优化：先批量收集所有 ID，一次性查询 =====
        Set<Long> senderIds = new HashSet<>();
        Set<Long> orderIds = new HashSet<>();
        Set<Long> messageIds = new HashSet<>();
        for (Message msg : records) {
            senderIds.add(msg.getSenderId());
            if (msg.getOrderId() != null) orderIds.add(msg.getOrderId());
            messageIds.add(msg.getId());
        }

        // 批量查 User（发送方）
        Map<Long, User> userMap = senderIds.isEmpty() ? new HashMap<>()
                : userService.listByIds(senderIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        // 批量查 Order（关联订单）
        Map<Long, Order> orderMap = orderIds.isEmpty() ? new HashMap<>()
                : orderService.listByIds(orderIds).stream()
                        .collect(Collectors.toMap(Order::getId, Function.identity()));

        // 批量查 Attachment（附件），按 messageId 分组
        Map<Long, List<MessageAttachment>> attachmentMap = new HashMap<>();
        if (!messageIds.isEmpty()) {
            LambdaQueryWrapper<MessageAttachment> attachWrapper = new LambdaQueryWrapper<>();
            attachWrapper.in(MessageAttachment::getMessageId, messageIds);
            List<MessageAttachment> allAttachments = attachmentMapper.selectList(attachWrapper);
            for (MessageAttachment att : allAttachments) {
                attachmentMap.computeIfAbsent(att.getMessageId(), k -> new ArrayList<>()).add(att);
            }
        }

        // ===== 构建 VO =====
        IPage<MessageVO> voPage = new Page<>(page, size);
        voPage.setTotal(messagePage.getTotal());

        List<MessageVO> voList = new ArrayList<>();
        for (Message msg : records) {
            MessageVO vo = new MessageVO();
            BeanUtils.copyProperties(msg, vo);
            vo.setIsMine(msg.getSenderId().equals(currentUserId));

            // 从 Map 取发送方信息（避免 N 次 getById）
            User sender = userMap.get(msg.getSenderId());
            if (sender != null) {
                vo.setSenderName(sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
                vo.setSenderAvatar(sender.getAvatar());
            }

            // 从 Map 取订单信息并验证权限
            if (msg.getOrderId() != null) {
                Order order = orderMap.get(msg.getOrderId());
                if (order != null && !order.getUserId().equals(currentUserId) && !order.getUserId().equals(otherUserId)) {
                    vo.setOrderId(null);
                }
            }

            // 从 Map 取附件（避免 N 次 selectList）
            List<MessageAttachment> attachments = attachmentMap.getOrDefault(msg.getId(), Collections.emptyList());
            if (!attachments.isEmpty()) {
                List<MessageAttachmentVO> attachVOList = new ArrayList<>();
                for (MessageAttachment att : attachments) {
                    MessageAttachmentVO attVO = new MessageAttachmentVO();
                    attVO.setId(att.getId());
                    attVO.setFileUrl(att.getFileUrl());
                    attVO.setFileName(att.getFileName());
                    attVO.setFileSize(att.getFileSize());
                    attachVOList.add(attVO);
                }
                vo.setAttachments(attachVOList);
            }

            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public MessageVO getMessageDetail(Long messageId, Long currentUserId) {
        Message msg = this.getById(messageId);
        if (msg == null) throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);

        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(msg, vo);
        vo.setIsMine(msg.getSenderId().equals(currentUserId));

        User sender = userService.getById(msg.getSenderId());
        if (sender != null) {
            vo.setSenderName(sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
            vo.setSenderAvatar(sender.getAvatar());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long sessionId, Long userId) {
        // 1. 标记所有消息为已读
        baseMapper.markAllAsRead(sessionId, userId);

        // 2. 清零会话未读计数
        sessionService.markSessionRead(sessionId, userId);
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0)
                .eq(Message::getIsDeleted, 0);
        return (int) this.count(wrapper);
    }
}