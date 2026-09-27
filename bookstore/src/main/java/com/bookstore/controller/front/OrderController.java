package com.bookstore.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.dto.OrderSubmitDTO;
import com.bookstore.entity.Order;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.OrderService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.OrderDetailVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    public Result<List<Long>> submit(@Valid @RequestBody OrderSubmitDTO dto) {
        return Result.success(orderService.submitOrder(dto));
    }

    // ===== 修改：orderId 改为 String =====
    @PostMapping("/pay/{orderId}")
    public Result<Void> pay(@PathVariable String orderId) {
        Long id = parseOrderId(orderId);
        orderService.payOrder(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<IPage<Order>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, UserContext.getUserId());
        if (status != null) wrapper.eq(Order::getStatus, status);
        wrapper.orderByDesc(Order::getCreateTime);
        return Result.success(orderService.page(new Page<>(page, size), wrapper));
    }

    // ===== 已修改：String orderId =====
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> detail(@PathVariable String orderId) {
        Long id = parseOrderId(orderId);
        OrderDetailVO vo = orderService.getOrderDetail(id);
        return Result.success(vo);
    }

    // ===== 修改：orderId 改为 String，业务逻辑下沉到 Service =====
    @PutMapping("/cancel/{orderId}")
    public Result<Void> cancel(@PathVariable String orderId) {
        Long id = parseOrderId(orderId);
        // Service 层已包含：归属校验 + 状态校验 + 回补库存 + 事务
        orderService.cancelOrder(id, UserContext.getUserId());
        return Result.success();
    }

    // ===== 修改：orderId 改为 String，业务逻辑下沉到 Service =====
    @PutMapping("/finish/{orderId}")
    public Result<Void> finish(@PathVariable String orderId) {
        Long id = parseOrderId(orderId);
        // Service 层已包含：归属校验 + 状态校验 + 改状态 + 事务
        orderService.finishOrder(id, UserContext.getUserId());
        return Result.success();
    }

    /**
     * 安全解析 orderId，防止非法输入导致 500 错误
     */
    private Long parseOrderId(String orderId) {
        try {
            return Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.ORDER_ID_FORMAT_ERROR);
        }
    }

    // ==================== 聊天订单交集（买家视角） ====================

    @GetMapping("/chat-orders")
    public Result<List<Order>> chatOrders(@RequestParam Long receiverId) {
        Long userId = UserContext.getUserId();
        List<Order> orders = orderService.getChatOrdersAsBuyer(userId, receiverId);
        return Result.success(orders);
    }
}