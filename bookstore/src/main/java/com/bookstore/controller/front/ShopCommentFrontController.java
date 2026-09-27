package com.bookstore.controller.front;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.common.Result;
import com.bookstore.dto.ShopCommentCreateDTO;
import com.bookstore.service.ShopCommentService;
import com.bookstore.service.ShopService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.ShopCommentVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shop-comment")
public class ShopCommentFrontController {

    @Autowired
    private ShopCommentService shopCommentService;

    @Autowired
    private ShopService shopService;

    /** 发表店铺评价 */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ShopCommentCreateDTO dto) {
        Long userId = UserContext.getUserId();
        shopCommentService.createComment(userId, dto);
        return Result.success();
    }

    /** 分页查询店铺评价（公开） */
    @GetMapping("/list")
    public Result<IPage<ShopCommentVO>> list(
            @RequestParam Long shopId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(shopCommentService.getShopComments(shopId, page, size));
    }

    /** 店铺评分统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam Long shopId) {
        return Result.success(shopCommentService.getShopRatingStats(shopId));
    }
}
