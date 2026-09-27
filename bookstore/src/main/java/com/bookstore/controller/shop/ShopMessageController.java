package com.bookstore.controller.shop;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.dto.SendMessageDTO;
import com.bookstore.entity.Message;
import com.bookstore.entity.Session;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.MessageService;
import com.bookstore.service.SessionService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.MessageVO;
import com.bookstore.vo.SessionVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop/message")
public class ShopMessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private SessionService sessionService;

    /**
     * 商家发送消息
     */
    @PostMapping("/send")
    public Result<Message> send(@Valid @RequestBody SendMessageDTO dto) {
        Long userId = UserContext.getUserId();
        Message message = messageService.sendMessage(userId, dto);
        return Result.success(message);
    }

    /**
     * 获取商家的会话列表
     */
    @GetMapping("/sessions")
    public Result<IPage<SessionVO>> sessions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = UserContext.getUserId();
        return Result.success(sessionService.getUserSessions(userId, page, size));
    }

    /**
     * 获取会话消息列表（分页）
     */
    @GetMapping("/list/{sessionId}")
    public Result<IPage<MessageVO>> list(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = UserContext.getUserId();
        // 自动标记为已读
        messageService.markAllAsRead(sessionId, userId);
        return Result.success(messageService.getSessionMessages(sessionId, userId, page, size));
    }

    /**
     * 获取未读消息总数
     */
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() {
        Long userId = UserContext.getUserId();
        return Result.success(messageService.getUnreadCount(userId));
    }

    /**
     * 获取或创建与指定用户的会话
     */
    @GetMapping("/create-or-get-session")
    public Result<Long> createOrGetSession(@RequestParam Long targetUserId) {
        Long userId = UserContext.getUserId();
        Session session = sessionService.getOrCreateSession(userId, targetUserId);
        return Result.success(session.getId());
    }

    /**
     * 获取会话中的对方用户ID（用于直接打开聊天URL的fallback）
     */
    @GetMapping("/session/{sessionId}/receiver")
    public Result<Long> getReceiver(@PathVariable Long sessionId) {
        Long userId = UserContext.getUserId();
        Long otherId = sessionService.getOtherUserId(sessionId, userId);
        if (otherId == null || otherId.equals(userId)) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在或无权访问");
        }
        return Result.success(otherId);
    }

    /** 删除当前用户视角的会话（软删，对方仍可见） */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        Long userId = UserContext.getUserId();
        sessionService.markUserDeleted(sessionId, userId);
        return Result.success();
    }
}