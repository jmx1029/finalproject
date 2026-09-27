package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.common.ErrorCode;
import com.bookstore.entity.Book;
import com.bookstore.entity.Comment;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;
import com.bookstore.entity.Shop;
import com.bookstore.entity.User;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.CommentMapper;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.mapper.OrderMapper;
import com.bookstore.service.BookService;
import com.bookstore.service.CommentService;
import com.bookstore.service.ShopService;
import com.bookstore.service.UserService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.CommentVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private UserService userService;

    @Autowired
    private BookService bookService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderMapper orderMapper;

    // ====== 前端：图书评论列表（只查顶级评论 + 每条带最近 5 条回复）======

    @Override
    public IPage<CommentVO> getCommentsByBookId(Long bookId, Integer page, Integer size, Long currentUserId) {
        // 1. 只查顶级评论（parent_id IS NULL）
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getBookId, bookId)
                .isNull(Comment::getParentId)
                .eq(Comment::getIsDeleted, 0)
                .orderByDesc(Comment::getCreateTime);

        IPage<Comment> topPage = this.page(new Page<>(page, size), wrapper);

        // 2. 把顶级评论转 VO，同时批量加载回复
        List<Comment> tops = topPage.getRecords();
        List<Long> topIds = tops.stream().map(Comment::getId).collect(Collectors.toList());

        // 2a. 批量查 replyCount（SQL 一次搞定）
        Map<Long, Integer> replyCountMap = batchCountReplies(topIds);

        // 2b. 批量查每条顶级评论的最近 5 条回复（N+1 规避：用 Map 分组）
        Map<Long, List<Comment>> repliesMap = batchLoadReplies(topIds, 5);

        // 3. 转 VO
        List<CommentVO> voList = new ArrayList<>();
        for (Comment top : tops) {
            CommentVO vo = convertToTopVO(top, currentUserId);
            vo.setReplyCount(replyCountMap.getOrDefault(top.getId(), 0));

            List<Comment> replies = repliesMap.getOrDefault(top.getId(), Collections.emptyList());
            vo.setReplies(convertReplies(replies));

            voList.add(vo);
        }

        IPage<CommentVO> voPage = new Page<>(topPage.getCurrent(), topPage.getSize());
        voPage.setTotal(topPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Comment addComment(Long userId, Long bookId, String content, Integer rating) {
        // 1. 购买校验：所有用户（含管理员）必须买过本书且订单已发货/已完成
        Integer exists = baseMapper.checkPurchased(bookId, userId);
        if (exists == null) {
            // === 阶段五 T7：改用专门错误码 COMMENT_PURCHASE_REQUIRED，不再复用 COMMENT_NOT_FOUND ===
            throw new BusinessException(ErrorCode.COMMENT_PURCHASE_REQUIRED);
        }

        // 2. 重复评价拦截：同一用户对同一本书只能有一条顶级评论
        Long existCount = this.count(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, userId)
                        .eq(Comment::getBookId, bookId)
                        .isNull(Comment::getParentId)
                        .eq(Comment::getIsDeleted, 0)
        );
        if (existCount > 0) {
            // === 阶段五 T7：改用专门错误码 COMMENT_DUPLICATE ===
            throw new BusinessException(ErrorCode.COMMENT_DUPLICATE);
        }

        Comment comment = new Comment();
        comment.setBookId(bookId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setRating(rating);
        comment.setLikeCount(0);
        this.save(comment);

        recalculateBookRating(bookId);
        return comment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId, Integer role) {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        // 仅本人或管理员可删除
        if (!comment.getUserId().equals(userId) && role != 1) {
            throw new BusinessException(ErrorCode.COMMENT_NO_PERMISSION_DELETE);
        }

        Long bookId = comment.getBookId();
        boolean isTopLevel = comment.getParentId() == null;

        comment.setIsDeleted(1);
        this.updateById(comment);

        // === 阶段三 T4：删除顶级评论时，同步软删其下所有回复，防止孤儿数据 ===
        if (isTopLevel) {
            LambdaQueryWrapper<Comment> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.eq(Comment::getParentId, comment.getId())
                    .eq(Comment::getIsDeleted, 0);
            List<Comment> replies = this.list(replyWrapper);
            for (Comment reply : replies) {
                reply.setIsDeleted(1);
            }
            if (!replies.isEmpty()) {
                this.updateBatchById(replies);
            }
            // 顶级评论删除才影响平均分（删除回复不影响评分）
            recalculateBookRating(bookId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId) {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        baseMapper.incrementLikeCount(commentId);
    }

    // ===== 用户回复（抖音风格）=====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Comment addReply(Long userId, Long parentId, Long replyToUserId, String content) {
        Comment parent = this.getById(parentId);
        if (parent == null || parent.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "回复的评论不存在");
        }

        // parentId 必须是顶级评论（简化：只支持一层嵌套）
        if (parent.getParentId() != null) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "仅支持在顶级评论下回复");
        }

        Comment reply = new Comment();
        reply.setBookId(parent.getBookId());
        reply.setUserId(userId);
        reply.setContent(content);
        reply.setLikeCount(0);
        reply.setParentId(parentId);
        reply.setReplyToUserId(replyToUserId != null ? replyToUserId : parent.getUserId());
        // 回复不设 rating（只有顶级评论有评分）
        this.save(reply);

        // 回复不影响 book.rating
        return reply;
    }

    @Override
    public IPage<CommentVO> getRepliesByParentId(Long parentId, Integer page, Integer size, Long currentUserId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getParentId, parentId)
                .eq(Comment::getIsDeleted, 0)
                .orderByAsc(Comment::getCreateTime);

        IPage<Comment> replyPage = this.page(new Page<>(page, size), wrapper);

        List<CommentVO> voList = convertReplies(replyPage.getRecords());
        IPage<CommentVO> voPage = new Page<>(replyPage.getCurrent(), replyPage.getSize());
        voPage.setTotal(replyPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ===== 商家端方法 =====

    @Override
    public IPage<CommentVO> getShopComments(Long shopId, Integer page, Integer size, Integer rating, String keyword) {
        LambdaQueryWrapper<Book> bookWrapper = new LambdaQueryWrapper<>();
        bookWrapper.eq(Book::getShopId, shopId)
                .eq(Book::getIsDeleted, 0)
                .select(Book::getId);
        List<Book> books = bookService.list(bookWrapper);
        List<Long> bookIds = books.stream().map(Book::getId).collect(Collectors.toList());

        if (bookIds.isEmpty()) {
            return new Page<>(page, size);
        }

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Comment::getBookId, bookIds)
                .isNull(Comment::getParentId)   // === 阶段三 T2：只查顶级评论，回复(reply)不能作为商家端的独立评价展示
                .eq(Comment::getIsDeleted, 0);

        if (rating != null) {
            if (rating == 4) wrapper.ge(Comment::getRating, 8);
            else if (rating == 2) wrapper.le(Comment::getRating, 4);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Comment::getContent, keyword);
        }
        wrapper.orderByDesc(Comment::getCreateTime);

        IPage<Comment> commentPage = this.page(new Page<>(page, size), wrapper);

        IPage<CommentVO> voPage = new Page<>(page, size);
        voPage.setTotal(commentPage.getTotal());

        List<CommentVO> voList = new ArrayList<>();
        for (Comment comment : commentPage.getRecords()) {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(comment, vo);
            User user = userService.getById(comment.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            Book book = bookService.getById(comment.getBookId());
            if (book != null) {
                vo.setBookTitle(book.getTitle());
                vo.setBookCover(book.getCoverUrl());
            }
            vo.setCanDelete(false);
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyComment(Long commentId, Long shopId, String content) {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        }
        Book book = bookService.getById(comment.getBookId());
        if (book == null || !book.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.COMMENT_NO_PERMISSION_REPLY);
        }
        comment.setReplyContent(content);
        comment.setReplyTime(LocalDateTime.now());
        this.updateById(comment);
    }

    @Override
    public CommentVO getCommentDetail(Long commentId, Long shopId) {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NOT_FOUND);
        }
        Book book = bookService.getById(comment.getBookId());
        if (book == null || !book.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.COMMENT_NO_PERMISSION_REPLY);
        }
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        User user = userService.getById(comment.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        vo.setBookTitle(book.getTitle());
        vo.setBookCover(book.getCoverUrl());
        vo.setCanDelete(false);
        return vo;
    }

    // ===== 辅助方法 =====

    /** 批量统计 replyCount（SQL IN 查询） */
    private Map<Long, Integer> batchCountReplies(List<Long> parentIds) {
        if (parentIds.isEmpty()) return Collections.emptyMap();

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Comment::getParentId, parentIds)
                .eq(Comment::getIsDeleted, 0)
                .select(Comment::getParentId);

        // 用 Stream 分组计数
        List<Comment> allReplies = this.list(wrapper);
        return allReplies.stream()
                .collect(Collectors.groupingBy(
                        Comment::getParentId,
                        Collectors.collectingAndThen(Collectors.counting(), c -> c.intValue())
                ));
    }

    /** 批量加载每条顶级评论的最近 N 条回复（避免 N+1） */
    private Map<Long, List<Comment>> batchLoadReplies(List<Long> parentIds, int limitPerParent) {
        if (parentIds.isEmpty()) return Collections.emptyMap();

        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Comment::getParentId, parentIds)
                .eq(Comment::getIsDeleted, 0)
                .orderByAsc(Comment::getCreateTime);

        List<Comment> allReplies = this.list(wrapper);

        // 按 parentId 分组 + 每组截断 limit
        return allReplies.stream()
                .collect(Collectors.groupingBy(
                        Comment::getParentId,
                        Collectors.toList()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            List<Comment> list = e.getValue();
                            if (list.size() > limitPerParent) {
                                return list.subList(0, limitPerParent);
                            }
                            return list;
                        }
                ));
    }

    /** 顶级评论 → VO */
    private CommentVO convertToTopVO(Comment comment, Long currentUserId) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);

        User user = userService.getById(comment.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }

        Integer role = UserContext.getRole();
        vo.setCanDelete(comment.getUserId().equals(currentUserId) || (role != null && role == 1));
        return vo;
    }

    /** 回复列表 → VO 列表（批量查被回复用户昵称） */
    private List<CommentVO> convertReplies(List<Comment> replies) {
        if (replies.isEmpty()) return Collections.emptyList();

        // 收集所有需要查的用户ID（发回复的 + 被回复的）
        Set<Long> userIds = new HashSet<>();
        for (Comment r : replies) {
            userIds.add(r.getUserId());
            if (r.getReplyToUserId() != null) userIds.add(r.getReplyToUserId());
        }
        Map<Long, User> userMap = new HashMap<>();
        for (Long uid : userIds) {
            User u = userService.getById(uid);
            if (u != null) userMap.put(uid, u);
        }

        List<CommentVO> voList = new ArrayList<>();
        for (Comment r : replies) {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(r, vo);

            User author = userMap.get(r.getUserId());
            if (author != null) {
                vo.setUsername(author.getUsername());
                vo.setNickname(author.getNickname());
                vo.setAvatar(author.getAvatar());
            }

            // 被回复人的昵称（抖音风格："回复 某用户"）
            if (r.getReplyToUserId() != null) {
                User replyTo = userMap.get(r.getReplyToUserId());
                if (replyTo != null) {
                    vo.setReplyToUsername(replyTo.getNickname() != null ? replyTo.getNickname() : replyTo.getUsername());
                }
            }

            voList.add(vo);
        }
        return voList;
    }

    /** 重算图书评分 */
    private void recalculateBookRating(Long bookId) {
        Map<String, Object> stats = baseMapper.getRatingAvgAndCount(bookId);
        if (stats == null) return;

        Object avgObj = stats.get("avg_rating");
        Object cntObj = stats.get("cnt");
        if (cntObj == null) return;

        Long cnt = ((Number) cntObj).longValue();
        // === 阶段三 T3：删光所有评论后把评分归零，防止残留历史爬虫分/平均分 ===
        if (cnt == 0) {
            bookMapper.updateRating(bookId, BigDecimal.ZERO);
            return;
        }
        if (avgObj == null) return;

        BigDecimal avgRating;
        if (avgObj instanceof BigDecimal) {
            avgRating = (BigDecimal) avgObj;
        } else {
            avgRating = new BigDecimal(avgObj.toString());
        }
        avgRating = avgRating.setScale(1, RoundingMode.HALF_UP);
        bookMapper.updateRating(bookId, avgRating);
    }

    @SuppressWarnings("unused")
    private IPage<CommentVO> convertToVOPage(IPage<Comment> commentPage, Long currentUserId) {
        // 保留兼容，商家端老逻辑还在用
        IPage<CommentVO> voPage = new Page<>(commentPage.getCurrent(), commentPage.getSize());
        voPage.setTotal(commentPage.getTotal());
        List<CommentVO> voList = new ArrayList<>();
        for (Comment comment : commentPage.getRecords()) {
            voList.add(convertToTopVO(comment, currentUserId));
        }
        voPage.setRecords(voList);
        return voPage;
    }
}
