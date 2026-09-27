package com.bookstore.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.common.Result;
import com.bookstore.dto.CommentCreateDTO;
import com.bookstore.dto.CommentReplyDTO;
import com.bookstore.entity.Comment;
import com.bookstore.mapper.CommentMapper;
import com.bookstore.service.CommentService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.CommentVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentMapper commentMapper;

    /**
     * 获取图书评论列表
     */
    @GetMapping("/list")
    public Result<IPage<CommentVO>> list(
            @RequestParam Long bookId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        return Result.success(commentService.getCommentsByBookId(bookId, page, size, userId));
    }

    /**
     * 预检：用户是否有资格对某本书发表评论
     * 返回 { purchased, alreadyCommented }
     */
    @GetMapping("/check-purchased")
    public Result<Map<String, Object>> checkPurchased(@RequestParam Long bookId) {
        Long userId = UserContext.getUserId();

        boolean purchased = commentMapper.checkPurchased(bookId, userId) != null;

        Long existCount = commentService.count(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, userId)
                        .eq(Comment::getBookId, bookId)
                        .isNull(Comment::getParentId)
                        .eq(Comment::getIsDeleted, 0)
        );
        boolean alreadyCommented = existCount > 0;

        Map<String, Object> result = new HashMap<>();
        result.put("purchased", purchased);
        result.put("alreadyCommented", alreadyCommented);
        return Result.success(result);
    }

    /**
     * === 阶段四 T4：发表评论（改用 CommentCreateDTO + @Valid，不再用 Map 手写解析）===
     * 所有字段校验（非空、长度、评分范围）由 @Valid 自动处理，异常由 GlobalExceptionHandler 转为 400
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody CommentCreateDTO dto) {
        Long userId = UserContext.getUserId();
        commentService.addComment(userId, dto.getBookId(), dto.getContent(), dto.getRating());
        return Result.success();
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        Integer role = UserContext.getRole();
        commentService.deleteComment(id, userId, role);
        return Result.success();
    }

    /**
     * 点赞评论
     */
    @PutMapping("/like/{id}")
    public Result<Void> like(@PathVariable Long id) {
        commentService.likeComment(id);
        return Result.success();
    }

    // ===== 用户回复（抖音风格）=====

    /**
     * === 阶段四 T4：发表回复（改用 CommentReplyDTO + @Valid，不再用 Map 手写解析）===
     * 所有字段校验由 @Valid 自动处理；Service 层会校验 parentId 是否存在且为顶级评论
     */
    @PostMapping("/reply")
    public Result<Void> addReply(@Valid @RequestBody CommentReplyDTO dto) {
        Long userId = UserContext.getUserId();
        commentService.addReply(userId, dto.getParentId(), dto.getReplyToUserId(), dto.getContent());
        return Result.success();
    }

    /**
     * 加载某条顶级评论的更多回复（分页）
     */
    @GetMapping("/replies/{parentId}")
    public Result<IPage<CommentVO>> getReplies(
            @PathVariable Long parentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        return Result.success(commentService.getRepliesByParentId(parentId, page, size, userId));
    }
}
