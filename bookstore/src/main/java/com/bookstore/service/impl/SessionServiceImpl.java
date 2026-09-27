package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.common.ErrorCode;
import com.bookstore.entity.Session;
import com.bookstore.entity.User;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.SessionMapper;
import com.bookstore.service.SessionService;
import com.bookstore.service.UserService;
import com.bookstore.vo.SessionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {

    @Autowired
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Session getOrCreateSession(Long user1Id, Long user2Id) {
        // === 阶段三 T5：拦截自己和自己聊天（自聊无意义）===
        if (user1Id != null && user1Id.equals(user2Id)) {
            throw new BusinessException(ErrorCode.MESSAGE_CONTENT_REQUIRED, "不能与自己建立会话");
        }

        // 确保 user1Id < user2Id，保证唯一性
        Long u1 = Math.min(user1Id, user2Id);
        Long u2 = Math.max(user1Id, user2Id);

        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getUser1Id, u1)
                .eq(Session::getUser2Id, u2)
                .eq(Session::getIsDeleted, 0);
        Session session = this.getOne(wrapper);

        if (session == null) {
            session = new Session();
            session.setUser1Id(u1);
            session.setUser2Id(u2);
            session.setUnreadCountUser1(0);
            session.setUnreadCountUser2(0);
            session.setUser1Deleted(0);
            session.setUser2Deleted(0);
            this.save(session);
        }

        return session;
    }

    @Override
    public IPage<SessionVO> getUserSessions(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Session::getUser1Id, userId).eq(Session::getUser1Deleted, 0)
                        .or().eq(Session::getUser2Id, userId).eq(Session::getUser2Deleted, 0))
                .eq(Session::getIsDeleted, 0)
                .orderByDesc(Session::getLastMessageTime);

        IPage<Session> sessionPage = this.page(new Page<>(page, size), wrapper);

        // 转换为 VO
        IPage<SessionVO> voPage = new Page<>(page, size);
        voPage.setTotal(sessionPage.getTotal());

        List<SessionVO> voList = new ArrayList<>();
        for (Session session : sessionPage.getRecords()) {
            Long targetUserId = session.getUser1Id().equals(userId) ? session.getUser2Id() : session.getUser1Id();
            User targetUser = userService.getById(targetUserId);

            SessionVO vo = new SessionVO();
            vo.setSessionId(session.getId());
            vo.setTargetUserId(targetUserId);
            if (targetUser != null) {
                vo.setTargetUsername(targetUser.getUsername());
                vo.setTargetNickname(targetUser.getNickname());
                vo.setTargetAvatar(targetUser.getAvatar());
                // 判断角色
                if (targetUser.getRole() != null && targetUser.getRole() == 1) {
                    vo.setTargetRole("admin");
                } else if (targetUser.getIsShopOwner() != null && targetUser.getIsShopOwner() == 1) {
                    vo.setTargetRole("shop");
                } else {
                    vo.setTargetRole("user");
                }
            }
            vo.setLastMessage(session.getLastMessage());
            vo.setLastMessageTime(session.getLastMessageTime());
            // 计算当前用户的未读数
            int unread = session.getUser1Id().equals(userId)
                    ? (session.getUnreadCountUser1() != null ? session.getUnreadCountUser1() : 0)
                    : (session.getUnreadCountUser2() != null ? session.getUnreadCountUser2() : 0);
            vo.setUnreadCount(unread);

            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public void updateLastMessage(Long sessionId, String content, Long senderId) {
        Session session = this.getById(sessionId);
        if (session != null) {
            session.setLastMessage(content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content);
            session.setLastMessageTime(LocalDateTime.now());
            this.updateById(session);
        }
    }

    @Override
    public void incrementUnreadCount(Long sessionId, Long receiverId) {
        Session session = this.getById(sessionId);
        if (session == null) return;

        // 使用 Mapper 原子 SQL 递增，防止并发丢更新
        if (session.getUser1Id().equals(receiverId)) {
            baseMapper.incrementUnreadUser1(sessionId);
        } else if (session.getUser2Id().equals(receiverId)) {
            baseMapper.incrementUnreadUser2(sessionId);
        }
    }

    @Override
    public void markSessionRead(Long sessionId, Long userId) {
        Session session = this.getById(sessionId);
        if (session == null) return;

        // 使用 Mapper 原子 SQL 清零，防止并发丢更新
        if (session.getUser1Id().equals(userId)) {
            baseMapper.clearUnreadUser1(sessionId);
        } else if (session.getUser2Id().equals(userId)) {
            baseMapper.clearUnreadUser2(sessionId);
        }
    }

    @Override
    public Long getOtherUserId(Long sessionId, Long currentUserId) {
        Session session = this.getById(sessionId);
        if (session == null) return null;
        return session.getUser1Id().equals(currentUserId) ? session.getUser2Id() : session.getUser1Id();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markUserDeleted(Long sessionId, Long currentUserId) {
        Session session = this.getById(sessionId);
        if (session == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND, "会话不存在");
        }

        // 归属校验：当前用户必须是会话参与方
        boolean isUser1 = session.getUser1Id().equals(currentUserId);
        boolean isUser2 = session.getUser2Id().equals(currentUserId);
        if (!isUser1 && !isUser2) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND, "无权删除该会话");
        }

        // 用 LambdaUpdateWrapper 条件更新，只 set 目标字段，避免全量覆盖并发风险
        LambdaUpdateWrapper<Session> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Session::getId, sessionId);

        // 标记对应删除位
        if (isUser1) {
            wrapper.set(Session::getUser1Deleted, 1);
        } else {
            wrapper.set(Session::getUser2Deleted, 1);
        }

        // 双方都删了 → 触发 isDeleted=1
        if ((isUser1 && session.getUser2Deleted() != null && session.getUser2Deleted() == 1)
                || (isUser2 && session.getUser1Deleted() != null && session.getUser1Deleted() == 1)) {
            wrapper.set(Session::getIsDeleted, 1);
        }

        wrapper.set(Session::getUpdateTime, java.time.LocalDateTime.now());
        this.update(null, wrapper);
    }
}