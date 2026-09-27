package com.bookstore.controller.shop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.dto.AfterSaleReviewDTO;
import com.bookstore.entity.AfterSale;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;
import com.bookstore.entity.Shop;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.service.AfterSaleService;
import com.bookstore.service.OrderService;
import com.bookstore.service.ShopService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.OrderDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shop/order")
public class ShopOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private AfterSaleService afterSaleService;

    @Autowired
    private OrderItemMapper orderItemMapper;

    // ==================== 列表（已有） ====================

    @GetMapping("/page")
    public Result<IPage<Order>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        Long userId = UserContext.getUserId();
        Long shopId = shopService.getShopIdByUserId(userId);
        if (shopId == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getShopId, shopId);
        if (status != null) wrapper.eq(Order::getStatus, status);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Order::getId, keyword)
                    .or().like(Order::getReceiverName, keyword)
                    .or().like(Order::getReceiverPhone, keyword));
        }
        wrapper.orderByDesc(Order::getCreateTime);

        IPage<Order> result = orderService.page(new Page<>(page, size), wrapper);
        return Result.success(result);
    }

    // ==================== 发货（业务逻辑下沉到 OrderService.shipOrder） ====================

    @PutMapping("/ship/{orderId}")
    public Result<Void> ship(@PathVariable String orderId) {
        Long id = parseOrderId(orderId);
        // Service 层已包含：角色分支(管理员直通/商家校验 shopId 归属) + status 校验 + 改状态
        orderService.shipOrder(id);
        return Result.success();
    }

    // ==================== 【新增】订单详情 ====================

    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> detail(@PathVariable String orderId) {
        Long id = parseOrderId(orderId);
        Long userId = UserContext.getUserId();
        Long shopId = shopService.getShopIdByUserId(userId);
        if (shopId == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        }

        // 1. 查订单 + 归属校验
        Order order = orderService.getById(id);
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        if (!order.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION);
        }

        // === 阶段五 T8：VO 组装统一走 Service 公共方法 ===
        return Result.success(orderService.buildOrderDetailVO(order));
    }

    // ==================== 【新增】处理退款/售后 ====================

    @PutMapping("/refund-handle/{orderId}")
    public Result<Void> refundHandle(@PathVariable String orderId,
                                     @RequestBody AfterSaleReviewDTO dto) {
        Long id = parseOrderId(orderId);
        Long userId = UserContext.getUserId();
        Long shopId = shopService.getShopIdByUserId(userId);
        if (shopId == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        }

        // 1. 查订单 + 归属校验
        Order order = orderService.getById(id);
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        if (!order.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION);
        }

        // 2. 查该订单下的售后单（取最新一条）
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSale::getOrderId, id)
               .eq(AfterSale::getShopId, shopId)
               .orderByDesc(AfterSale::getCreateTime);
        AfterSale afterSale = afterSaleService.getOne(wrapper);
        if (afterSale == null) {
            throw new BusinessException(ErrorCode.AFTER_SALE_NOT_FOUND);
        }

        // 3. 复用已有的商家审核逻辑（dto.status: 1=接受, 2=拒绝）
        afterSaleService.shopReview(afterSale.getId(), shopId, dto.getStatus(), dto.getRemark());

        // 4. 同步更新订单状态：拒绝 → 变为"已关闭（售后驳回）"
        if (dto.getStatus() == 2) {
            order.setStatus(Order.STATUS_CLOSED);
            order.setUpdateTime(LocalDateTime.now());
            orderService.updateById(order);
        }

        return Result.success();
    }

    // ==================== 聊天订单交集（商家视角） ====================

    @GetMapping("/chat-orders")
    public Result<List<Order>> chatOrders(@RequestParam Long receiverId) {
        Long userId = UserContext.getUserId();
        List<Order> orders = orderService.getChatOrdersAsShop(userId, receiverId);
        return Result.success(orders);
    }

    // ==================== 工具方法 ====================

    private Long parseOrderId(String orderId) {
        try {
            return Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.ORDER_ID_FORMAT_ERROR);
        }
    }
}
