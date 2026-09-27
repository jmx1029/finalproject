package com.bookstore.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.dto.SendMessageDTO;
import com.bookstore.entity.Message;
import com.bookstore.entity.Session;
import com.bookstore.entity.User;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.SessionMapper;
import com.bookstore.service.MessageService;
import com.bookstore.service.SessionService;
import com.bookstore.service.UserService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.MessageVO;
import com.bookstore.vo.SessionVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/message")
public class AdminMessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserService userService;

    @Autowired
    private SessionMapper sessionMapper;   // ← 新增注入

    /**
     * 管理员发送消息（平台客服回复）
     */
    @PostMapping("/send")
    public Result<Message> send(@Valid @RequestBody SendMessageDTO dto) {
        Long userId = UserContext.getUserId();
        Message message = messageService.sendMessage(userId, dto);
        return Result.success(message);
    }

    /**
     * 管理员查看消息列表获取管理员的会话列表（我的会话）
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

    /**
     * 获取所有会话（管理员全局视角，不限参与方）
     */
    @GetMapping("/all-sessions")
    public Result<IPage<SessionVO>> allSessions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        // 使用 SessionMapper 直接分页查询
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getIsDeleted, 0)
                .orderByDesc(Session::getLastMessageTime);
        IPage<Session> sessionPage = sessionMapper.selectPage(new Page<>(page, size), wrapper);

        // 转换为 SessionVO
        IPage<SessionVO> voPage = new Page<>(page, size);
        voPage.setTotal(sessionPage.getTotal());

        List<SessionVO> voList = new ArrayList<>();
        for (Session session : sessionPage.getRecords()) {
            SessionVO vo = new SessionVO();
            vo.setSessionId(session.getId());
            vo.setLastMessage(session.getLastMessage());
            vo.setLastMessageTime(session.getLastMessageTime());

            User u1 = userService.getById(session.getUser1Id());
            User u2 = userService.getById(session.getUser2Id());
            vo.setTargetUserId(session.getUser1Id());  // 存一个作为代表

            String displayName = "";
            String roleDisplay = "";
            if (u1 != null && u2 != null) {
                String name1 = u1.getNickname() != null ? u1.getNickname() : u1.getUsername();
                String name2 = u2.getNickname() != null ? u2.getNickname() : u2.getUsername();
                displayName = name1 + " ↔ " + name2;
                roleDisplay = getUserRole(u1) + " ↔ " + getUserRole(u2);
            } else if (u1 != null) {
                displayName = u1.getNickname() != null ? u1.getNickname() : u1.getUsername();
                roleDisplay = getUserRole(u1);
            } else if (u2 != null) {
                displayName = u2.getNickname() != null ? u2.getNickname() : u2.getUsername();
                roleDisplay = getUserRole(u2);
            } else {
                displayName = "未知用户";
                roleDisplay = "未知";
            }
            vo.setTargetNickname(displayName);
            vo.setTargetUsername(displayName);
            vo.setTargetRole(roleDisplay);
            vo.setUnreadCount(0);  // 全局视角不显示未读数
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    // 辅助方法：判断用户角色
    private String getUserRole(User user) {
        if (user == null) return "未知";
        if (user.getRole() != null && user.getRole() == 1) return "管理员";
        if (user.getIsShopOwner() != null && user.getIsShopOwner() == 1) return "商家";
        return "用户";
    }

    /** 删除当前管理员视角的会话（软删） */
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        Long userId = UserContext.getUserId();
        sessionService.markUserDeleted(sessionId, userId);
        return Result.success();
    }
}