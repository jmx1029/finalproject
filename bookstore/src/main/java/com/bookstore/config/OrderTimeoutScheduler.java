package com.bookstore.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.entity.Order;
import com.bookstore.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待付款订单超时自动取消定时任务
 * <p>
 * 背景：下单时已原子扣减库存（decreaseStock），若用户迟迟不付款，库存会被永久占用。
 * 本任务扫描"待付款(status=0) 且创建时间超过 30 分钟"的订单，自动取消并回补库存。
 * <p>
 * 并发安全：cancelOrder 内含 status 校验（status 必须为 0 才能取消），
 * 若任务扫描到订单后用户恰好付款成功（status→1），cancelOrder 会抛出 ORDER_CANCEL_INVALID，
 * 本类 catch 后继续处理下一条，不会造成"已付款订单被取消"的矛盾状态。
 */
@Slf4j
@Component
public class OrderTimeoutScheduler {

    @Autowired
    private OrderService orderService;

    /** 待付款超时阈值：30 分钟 */
    private static final int TIMEOUT_MINUTES = 30;

    /**
     * 每 5 分钟扫描一次待付款超时订单
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        List<Order> timeoutOrders = orderService.list(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 0)          // 待付款
                .lt(Order::getCreateTime, deadline)  // 超时
                .eq(Order::getIsDeleted, 0));

        if (timeoutOrders.isEmpty()) {
            return;
        }

        int successCount = 0;
        int failCount = 0;
        for (Order order : timeoutOrders) {
            try {
                // userId=null 跳过归属校验（系统自动取消，非买家行为）
                // cancelOrder 内部会再次校验 status==0，防止"刚查到就付款"的竞态
                orderService.cancelOrder(order.getId(), null);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("[OrderTimeout] 自动取消订单失败 orderId={}, error={}", order.getId(), e.getMessage());
            }
        }
        log.info("[OrderTimeout] 扫描完成：超时订单 {} 条，成功取消 {} 条，失败 {} 条",
                timeoutOrders.size(), successCount, failCount);
    }
}
