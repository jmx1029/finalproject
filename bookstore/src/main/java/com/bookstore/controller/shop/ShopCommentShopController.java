package com.bookstore.controller.shop;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.ShopCommentService;
import com.bookstore.service.ShopService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.ShopCommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/shop/shop-comment")
public class ShopCommentShopController {

    @Autowired
    private ShopCommentService shopCommentService;

    @Autowired
    private ShopService shopService;

    /** 商家查看自己店铺的评价 */
    @GetMapping("/page")
    public Result<IPage<ShopCommentVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = UserContext.getUserId();
        Long shopId = shopService.getShopIdByUserId(userId);
        if (shopId == null) {
            throw new BusinessException(ErrorCode.NOT_SHOP_OWNER);
        }
        return Result.success(shopCommentService.getShopComments(shopId, page, size));
    }

    /** 商家回复评价 */
    @PutMapping("/reply/{commentId}")
    public Result<Void> reply(@PathVariable Long commentId, @RequestBody Map<String, String> body) {
        Long userId = UserContext.getUserId();
        Long shopId = shopService.getShopIdByUserId(userId);
        if (shopId == null) {
            throw new BusinessException(ErrorCode.NOT_SHOP_OWNER);
        }
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "回复内容不能为空");
        }
        shopCommentService.replyComment(commentId, shopId, content.trim());
        return Result.success();
    }
}
