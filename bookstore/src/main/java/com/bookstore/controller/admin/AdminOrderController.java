package com.bookstore.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;
import com.bookstore.entity.OrderRemark;
import com.bookstore.entity.Shop;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.mapper.OrderRemarkMapper;
import com.bookstore.service.OrderService;
import com.bookstore.service.ShopService;
import com.bookstore.service.UserService;
import com.bookstore.util.ExcelUtil;
import com.bookstore.util.UserContext;
import com.bookstore.vo.OrderDetailVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/order")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderRemarkMapper orderRemarkMapper;

    @Autowired
    private ShopService shopService;

    @Autowired
    private UserService userService;

    // ==================== 列表（已有） ====================

    @GetMapping("/page")
    public Result<IPage<Order>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        Long orderIdLong = parseOrderIdOrNull(orderId);
        return Result.success(orderService.pageQuery(page, size, orderIdLong, userId, status, startTime, endTime, minAmount, maxAmount));
    }

    @PutMapping("/ship/{orderId}")
    public Result<Void> ship(@PathVariable String orderId) {
        Long id = parseOrderId(orderId);
        orderService.shipOrder(id);
        return Result.success();
    }

    @GetMapping("/export")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) throws Exception {

        Long orderIdLong = parseOrderIdOrNull(orderId);
        List<Order> orders = orderService.getOrdersForExport(orderIdLong, userId, status, startTime, endTime, minAmount, maxAmount);
        ExcelUtil.exportOrders(response, orders, "订单数据_" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.success(orderService.getStatistics());
    }

    // ==================== 【新增】订单详情（无归属校验，管理员可看所有） ====================

    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> detail(@PathVariable String orderId) {
        Long id = parseOrderId(orderId);

        Order order = orderService.getById(id);
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);

        // === 阶段五 T8：VO 组装统一走 Service 公共方法 ===
        return Result.success(orderService.buildOrderDetailVO(order));
    }

    // ==================== 【新增】订单备注（管理员内部记录） ====================

    @PostMapping("/remark/{orderId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addRemark(@PathVariable String orderId,
                                  @Valid @RequestBody OrderRemarkCreateDTO dto) {
        Long id = parseOrderId(orderId);
        Order order = orderService.getById(id);
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);

        Long adminId = UserContext.getUserId();
        OrderRemark remark = new OrderRemark();
        remark.setOrderId(id);
        remark.setAdminId(adminId);
        remark.setAdminName(resolveAdminName(adminId));
        remark.setContent(dto.getContent());
        remark.setCreateTime(LocalDateTime.now());
        orderRemarkMapper.insert(remark);

        return Result.success();
    }

    // 查订单备注列表
    @GetMapping("/remark/list/{orderId}")
    public Result<List<OrderRemark>> listRemarks(@PathVariable String orderId) {
        Long id = parseOrderId(orderId);
        LambdaQueryWrapper<OrderRemark> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderRemark::getOrderId, id).orderByDesc(OrderRemark::getCreateTime);
        return Result.success(orderRemarkMapper.selectList(wrapper));
    }

    // ==================== 【新增】强制关闭异常订单 ====================

    @PutMapping("/close/{orderId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> close(@PathVariable String orderId,
                              @RequestBody(required = false) Map<String, String> body) {
        Long id = parseOrderId(orderId);
        Order order = orderService.getById(id);
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);

        // === 阶段二修复 + T6魔法数字修复 ===
        // 已支付/已发货/售后中的订单禁止直接强制关闭，必须走售后退款流程
        if (order.getStatus() == Order.STATUS_PAID
                || order.getStatus() == Order.STATUS_SHIPPED
                || order.getStatus() == Order.STATUS_AFTER_SALE) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "已支付/已发货/售后中的订单不能强制关闭，请走售后退款流程");
        }
        // 仅待付款订单允许强制关闭
        if (order.getStatus() != Order.STATUS_WAIT_PAY) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "仅待付款订单允许强制关闭");
        }

        String reason = body != null ? body.get("reason") : null;
        order.setStatus(Order.STATUS_CLOSED);
        order.setUpdateTime(LocalDateTime.now());
        orderService.updateById(order);

        // 自动记一条备注
        Long adminId = UserContext.getUserId();
        OrderRemark remark = new OrderRemark();
        remark.setOrderId(id);
        remark.setAdminId(adminId);
        remark.setAdminName(resolveAdminName(adminId));
        remark.setContent("管理员强制关闭订单：" + (reason != null ? reason : "异常处理"));
        remark.setCreateTime(LocalDateTime.now());
        orderRemarkMapper.insert(remark);

        return Result.success();
    }

    // ==================== DTO（内部类，仅本 Controller 用） ====================

    // ==================== 聊天订单交集（管理员视角） ====================

    @GetMapping("/chat-orders")
    public Result<List<Order>> chatOrders(@RequestParam Long receiverId) {
        // 入口鉴权：必须是管理员（role == 1）
        Integer role = UserContext.getRole();
        if (role == null || role != 1) {
            throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION, "仅管理员可访问该接口");
        }
        // 业务逻辑下沉到 Service
        return Result.success(orderService.getChatOrdersAsAdmin(receiverId));
    }

    @lombok.Data
    public static class OrderRemarkCreateDTO {
        @NotBlank(message = "备注内容不能为空")
        @Size(max = 500, message = "备注长度不能超过 500")
        private String content;
    }

    // ==================== 工具方法 ====================

    private Long parseOrderId(String orderId) {
        try {
            return Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.ORDER_ID_FORMAT_ERROR);
        }
    }

    private Long parseOrderIdOrNull(String orderId) {
        if (orderId == null) return null;
        return parseOrderId(orderId);
    }

    private String resolveAdminName(Long adminId) {
        try {
            com.bookstore.entity.User u = userService.getById(adminId);
            if (u != null && u.getNickname() != null) return u.getNickname();
            if (u != null && u.getUsername() != null) return u.getUsername();
        } catch (Exception ignored) {}
        return "管理员";
    }
}
