package com.bookstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.dto.ShopCommentCreateDTO;
import com.bookstore.entity.ShopComment;
import com.bookstore.vo.ShopCommentVO;

import java.util.Map;

public interface ShopCommentService extends IService<ShopComment> {
    /** 发表店铺评价 */
    void createComment(Long userId, ShopCommentCreateDTO dto);

    /** 分页查询某店铺的评价（公开） */
    IPage<ShopCommentVO> getShopComments(Long shopId, Integer page, Integer size);

    /** 商家回复评价 */
    void replyComment(Long commentId, Long shopId, String content);

    /** 店铺评分统计（avgRating + ratingCount） */
    Map<String, Object> getShopRatingStats(Long shopId);
}
