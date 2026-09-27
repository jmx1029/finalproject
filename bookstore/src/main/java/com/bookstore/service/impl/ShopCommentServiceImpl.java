package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.common.ErrorCode;
import com.bookstore.dto.ShopCommentCreateDTO;
import com.bookstore.entity.Shop;
import com.bookstore.entity.ShopComment;
import com.bookstore.entity.User;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.ShopCommentMapper;
import com.bookstore.service.ShopCommentService;
import com.bookstore.service.ShopService;
import com.bookstore.service.UserService;
import com.bookstore.vo.ShopCommentVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShopCommentServiceImpl extends ServiceImpl<ShopCommentMapper, ShopComment> implements ShopCommentService {

    @Autowired
    private ShopService shopService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createComment(Long userId, ShopCommentCreateDTO dto) {
        // 1. 店铺必须存在且正常
        Shop shop = shopService.getById(dto.getShopId());
        if (shop == null || (shop.getStatus() != null && shop.getStatus() == 0)) {
            throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        }

        // 2. 评分范围校验
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 10) {
            throw new BusinessException(ErrorCode.SHOP_COMMENT_RATING_INVALID);
        }

        // 3. 防重复：同一用户对同一店铺（可选同一订单）只能评一次
        LambdaQueryWrapper<ShopComment> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(ShopComment::getShopId, dto.getShopId())
                    .eq(ShopComment::getUserId, userId)
                    .eq(ShopComment::getIsDeleted, 0);
        // 如果带 orderId，则还检查订单维度
        if (dto.getOrderId() != null) {
            existWrapper.eq(ShopComment::getOrderId, dto.getOrderId());
        }
        long existCount = this.count(existWrapper);
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.SHOP_COMMENT_ALREADY_EXISTS);
        }

        // 4. 保存评价
        ShopComment comment = new ShopComment();
        BeanUtils.copyProperties(dto, comment);
        comment.setUserId(userId);
        comment.setLikeCount(0);
        comment.setIsDeleted(0);
        this.save(comment);

        // 5. 更新店铺评分（原子重算）
        refreshShopRating(dto.getShopId());
    }

    @Override
    public IPage<ShopCommentVO> getShopComments(Long shopId, Integer page, Integer size) {
        // 店铺必须存在
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        }

        LambdaQueryWrapper<ShopComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopComment::getShopId, shopId)
                .eq(ShopComment::getIsDeleted, 0)
                .orderByDesc(ShopComment::getCreateTime);

        IPage<ShopComment> commentPage = this.page(new Page<>(page, size), wrapper);
        List<ShopComment> records = commentPage.getRecords();

        // 批量查 User 避免 N+1
        Set<Long> userIds = records.stream().map(ShopComment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? new HashMap<>()
                : userService.listByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        IPage<ShopCommentVO> voPage = new Page<>(page, size);
        voPage.setTotal(commentPage.getTotal());
        List<ShopCommentVO> voList = new ArrayList<>();
        for (ShopComment comment : records) {
            ShopCommentVO vo = new ShopCommentVO();
            BeanUtils.copyProperties(comment, vo);
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setUsername(user.getUsername());
                vo.setAvatar(user.getAvatar());
            }
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyComment(Long commentId, Long shopId, String content) {
        ShopComment comment = this.getById(commentId);
        if (comment == null || comment.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.SHOP_COMMENT_NOT_FOUND);
        }
        // 权限：只有该店铺能回复
        if (!comment.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.SHOP_COMMENT_NO_PERMISSION);
        }
        comment.setReplyContent(content);
        comment.setReplyTime(LocalDateTime.now());
        this.updateById(comment);
    }

    @Override
    public Map<String, Object> getShopRatingStats(Long shopId) {
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("avgRating", shop.getAvgRating() != null ? shop.getAvgRating() : 0.0);
        stats.put("ratingCount", shop.getRatingCount() != null ? shop.getRatingCount() : 0);

        // 额外算一下评分分布（好评数/中评数/差评数）
        LambdaQueryWrapper<ShopComment> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(ShopComment::getShopId, shopId).eq(ShopComment::getIsDeleted, 0);
        List<ShopComment> all = this.list(allWrapper);
        long good = all.stream().filter(c -> c.getRating() != null && c.getRating() >= 8).count();
        long mid = all.stream().filter(c -> c.getRating() != null && c.getRating() >= 6 && c.getRating() < 8).count();
        long bad = all.stream().filter(c -> c.getRating() != null && c.getRating() < 6).count();
        stats.put("goodCount", good);
        stats.put("midCount", mid);
        stats.put("badCount", bad);

        return stats;
    }

    /**
     * 重新计算店铺评分（每次新增评价后调用）
     */
    private void refreshShopRating(Long shopId) {
        LambdaQueryWrapper<ShopComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopComment::getShopId, shopId)
               .eq(ShopComment::getIsDeleted, 0)
               .isNotNull(ShopComment::getRating);
        List<ShopComment> comments = this.list(wrapper);
        if (comments.isEmpty()) return;

        double avg = comments.stream()
                .mapToInt(ShopComment::getRating)
                .average()
                .orElse(0.0);
        BigDecimal rounded = BigDecimal.valueOf(Math.round(avg * 10) / 10.0); // 保留一位小数

        Shop shop = shopService.getById(shopId);
        shop.setAvgRating(rounded);
        shop.setRatingCount(comments.size());
        shopService.updateById(shop);
    }
}
