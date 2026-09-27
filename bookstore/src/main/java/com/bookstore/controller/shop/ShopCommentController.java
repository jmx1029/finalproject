package com.bookstore.controller.shop;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.entity.Comment;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.CommentService;
import com.bookstore.service.ShopService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop/comment")
public class ShopCommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private ShopService shopService;

    /**
     * 商家分页查询本店评价
     */
    @GetMapping("/page")
    public Result<IPage<CommentVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer rating,  // 评分筛选（10分制：0全部，4好评≥8分，2差评≤4分）
            @RequestParam(required = false) String keyword) {
        Long userId = UserContext.getUserId();
        Long shopId = shopService.getShopIdByUserId(userId);
        if (shopId == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        }
        return Result.success(commentService.getShopComments(shopId, page, size, rating, keyword));
    }

    /**
     * 商家回复评价
     */
    @PutMapping("/reply/{commentId}")
    public Result<Void> reply(
            @PathVariable Long commentId,
            @RequestParam String content) {
        Long userId = UserContext.getUserId();
        Long shopId = shopService.getShopIdByUserId(userId);
        if (shopId == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        }
        commentService.replyComment(commentId, shopId, content);
        return Result.success();
    }

    /**
     * 获取评价详情
     */
    @GetMapping("/{commentId}")
    public Result<CommentVO> detail(@PathVariable Long commentId) {
        Long userId = UserContext.getUserId();
        Long shopId = shopService.getShopIdByUserId(userId);
        if (shopId == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        }
        CommentVO vo = commentService.getCommentDetail(commentId, shopId);
        return Result.success(vo);
    }
}