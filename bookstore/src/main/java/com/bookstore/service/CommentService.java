package com.bookstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.entity.Comment;
import com.bookstore.vo.CommentVO;

public interface CommentService extends IService<Comment> {
    /**
     * 获取图书的评论列表（分页）
     */
    IPage<CommentVO> getCommentsByBookId(Long bookId, Integer page, Integer size, Long currentUserId);

    /**
     * 发表评论
     */
    Comment addComment(Long userId, Long bookId, String content, Integer rating);

    /**
     * 删除评论（仅本人或管理员）
     */
    void deleteComment(Long commentId, Long userId, Integer role);

    /**
     * 点赞评论
     */
    void likeComment(Long commentId);

    // ===== 新增：商家端方法 =====

    /**
     * 商家分页查询本店评价
     */
    IPage<CommentVO> getShopComments(Long shopId, Integer page, Integer size, Integer rating, String keyword);

    /**
     * 商家回复评价
     */
    void replyComment(Long commentId, Long shopId, String content);

    /**
     * 商家获取评价详情
     */
    CommentVO getCommentDetail(Long commentId, Long shopId);

    // ===== 用户回复（抖音风格） =====

    /**
     * 用户发表回复（回复顶级评论或回复另一条回复）
     * replyToUserId：被回复的用户ID（可以等于 parent 的用户，也可以是中间层用户）
     */
    Comment addReply(Long userId, Long parentId, Long replyToUserId, String content);

    /**
     * 加载某条顶级评论的更多回复（分页）
     */
    IPage<CommentVO> getRepliesByParentId(Long parentId, Integer page, Integer size, Long currentUserId);

}