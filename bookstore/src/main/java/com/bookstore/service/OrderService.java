package com.bookstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.dto.OrderSubmitDTO;
import com.bookstore.entity.Order;
import com.bookstore.vo.OrderDetailVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrderService extends IService<Order> {

    // ===== 原有方法（保留） =====
    List<Long> submitOrder(OrderSubmitDTO dto);

    void payOrder(Long orderId);

    void shipOrder(Long orderId);

    /**
     * 取消订单（买家端）：归属校验 + 状态校验 + 回补库存 + 事务
     */
    void cancelOrder(Long orderId, Long userId);

    /**
     * 确认收货（买家端）：归属校验 + 状态校验 + 事务
     */
    void finishOrder(Long orderId, Long userId);

    OrderDetailVO getOrderDetail(Long orderId);   // ✅ 保留！不能删！

    /**
     * === 阶段五 T8：公共 VO 组装方法，消除 ShopOrderController/AdminOrderController/OrderServiceImpl 里的重复拼装代码 ===
     * 接受一个已查出的 Order 对象，返回组装好的 OrderDetailVO（含 orderItems + shopName）。
     * 调用方自行负责"查订单 + 归属校验"，本方法只做纯数据拼装。
     */
    OrderDetailVO buildOrderDetailVO(Order order);

    // ===== 新增：高级筛选分页查询 =====
    IPage<Order> pageQuery(Integer page, Integer size,
                           Long orderId, Long userId,
                           Integer status,
                           LocalDateTime startTime, LocalDateTime endTime,
                           BigDecimal minAmount, BigDecimal maxAmount);

    // ===== 新增：导出订单列表 =====
    List<Order> getOrdersForExport(Long orderId, Long userId,
                                   Integer status,
                                   LocalDateTime startTime, LocalDateTime endTime,
                                   BigDecimal minAmount, BigDecimal maxAmount);

    // ===== 新增：统计报表数据 =====
    Map<String, Object> getStatistics();

    // ===== 聊天订单交集查询 =====
    /** 买家视角：查"我下单、对方店铺/管理员"的交集订单 */
    List<Order> getChatOrdersAsBuyer(Long myUserId, Long receiverId);
    /** 商家视角：查"我店铺、对方用户/管理员"的交集订单 */
    List<Order> getChatOrdersAsShop(Long myUserId, Long receiverId);
    /** 管理员视角：按对方(receiver)角色过滤——商家→该店铺全部订单，普通用户→该用户全部订单 */
    List<Order> getChatOrdersAsAdmin(Long receiverId);
}