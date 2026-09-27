package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.common.ErrorCode;
import com.bookstore.dto.AfterSaleApplyDTO;
import com.bookstore.entity.*;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.AfterSaleLogMapper;
import com.bookstore.mapper.AfterSaleMapper;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.service.AfterSaleService;
import com.bookstore.service.BookService;
import com.bookstore.service.OrderService;
import com.bookstore.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AfterSaleServiceImpl extends ServiceImpl<AfterSaleMapper, AfterSale> implements AfterSaleService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private AfterSaleLogMapper afterSaleLogMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private BookService bookService;

    @Autowired
    private com.bookstore.mapper.BookMapper bookMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long userId, AfterSaleApplyDTO dto) {
        // 1. 将 String 类型的 orderId 转为 Long（防止前端精度丢失 + NumberFormatException → 业务异常）
        Long orderId;
        try {
            orderId = Long.parseLong(dto.getOrderId());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.ORDER_ID_FORMAT_ERROR);
        }

        // 2. 校验订单
        Order order = orderService.getById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        if (!order.getUserId().equals(userId)) throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION);

        // 3. 订单状态校验：允许申请售后的状态
        // 已支付(1)、已发货(2)、已完成(3)、已关闭-售后驳回(7) 可申请
        if (!Arrays.asList(
                Order.STATUS_PAID,
                Order.STATUS_SHIPPED,
                Order.STATUS_COMPLETED,
                Order.STATUS_CLOSED
        ).contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.AFTER_SALE_STATUS_INVALID);
        }

        // 4. 15天时限（仅当订单已完成或已关闭）
        if ((order.getStatus() == Order.STATUS_COMPLETED || order.getStatus() == Order.STATUS_CLOSED)
                && order.getFinishTime() != null) {
            LocalDateTime finishTime = order.getFinishTime();
            if (finishTime.plusDays(15).isBefore(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.AFTER_SALE_TIMEOUT);
            }
        }

        // 5. 检查是否已有进行中的售后单（状态0待审核 / 1商家通过 / 3待仲裁）
        long count = this.count(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderId, orderId)
                .in(AfterSale::getStatus, 0, 1, 3));
        if (count > 0) throw new BusinessException(ErrorCode.AFTER_SALE_IN_PROGRESS);

        // 6. 如果订单状态为"已关闭（售后驳回）"，重新申请时重置订单状态为"售后中"
        if (order.getStatus() == Order.STATUS_CLOSED) {
            order.setStatus(Order.STATUS_AFTER_SALE);
            order.setUpdateTime(LocalDateTime.now());
            orderService.updateById(order);
        }

        // 7. 构建售后单
        AfterSale as = new AfterSale();
        as.setOrderId(orderId);
        as.setUserId(userId);
        as.setShopId(order.getShopId());
        as.setType(dto.getType());
        as.setReason(dto.getReason());
        as.setDescription(dto.getDescription());
        // 退款金额校验：不能超过订单总额
        java.math.BigDecimal amount = dto.getAmount() != null ? dto.getAmount() : order.getTotalAmount();
        if (amount.compareTo(order.getTotalAmount()) > 0) {
            throw new BusinessException(ErrorCode.AFTER_SALE_AMOUNT_EXCEED);
        }
        as.setAmount(amount);
        as.setStatus(0);
        as.setApplyTime(LocalDateTime.now());
        this.save(as);

        // 8. 如果订单状态还不是"售后中"，则更新为"售后中"
        if (order.getStatus() != Order.STATUS_AFTER_SALE) {
            order.setStatus(Order.STATUS_AFTER_SALE);
            order.setUpdateTime(LocalDateTime.now());
            orderService.updateById(order);
        }

        // 9. 记录日志
        AfterSaleLog log = new AfterSaleLog();
        log.setAfterSaleId(as.getId());
        log.setOperator("user");
        log.setOperatorId(userId);
        log.setAction("apply");
        log.setRemark("用户发起售后申请");
        afterSaleLogMapper.insert(log);
    }

    @Override
    public IPage<AfterSale> getUserAfterSales(Long userId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSale::getUserId, userId);
        if (status != null) wrapper.eq(AfterSale::getStatus, status);
        wrapper.orderByDesc(AfterSale::getCreateTime);
        return this.page(new Page<>(page, size), wrapper);
    }

    @Override
    public IPage<AfterSale> getShopAfterSales(Long shopId, Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSale::getShopId, shopId);
        if (status != null) wrapper.eq(AfterSale::getStatus, status);
        wrapper.orderByDesc(AfterSale::getCreateTime);
        return this.page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shopReview(Long afterSaleId, Long shopId, Integer status, String remark) {
        AfterSale as = this.getById(afterSaleId);
        if (as == null) throw new BusinessException(ErrorCode.AFTER_SALE_NOT_FOUND);
        if (!as.getShopId().equals(shopId)) throw new BusinessException(ErrorCode.AFTER_SALE_NO_PERMISSION);
        if (as.getStatus() != 0) throw new BusinessException(ErrorCode.AFTER_SALE_ALREADY_PROCESSED);

        if (status == 1) {
            as.setStatus(1);
        } else if (status == 2) {
            as.setStatus(2);
        } else {
            throw new BusinessException(ErrorCode.AFTER_SALE_REVIEW_STATUS_INVALID);
        }
        as.setReviewTime(LocalDateTime.now());
        as.setShopReviewRemark(remark);
        this.updateById(as);

        AfterSaleLog log = new AfterSaleLog();
        log.setAfterSaleId(afterSaleId);
        log.setOperator("shop");
        log.setOperatorId(shopId);
        log.setAction(status == 1 ? "review_agree" : "review_reject");
        log.setRemark(status == 1 ? "商家审核通过" : "商家驳回：" + (remark != null ? remark : ""));
        afterSaleLogMapper.insert(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminArbitrate(Long afterSaleId, Integer status, String remark) {
        // 1. 获取售后单
        AfterSale as = this.getById(afterSaleId);
        if (as == null) throw new BusinessException(ErrorCode.AFTER_SALE_NOT_FOUND);
        // ===== 修改：只有已完结（4、5）才不可操作，其他状态管理员均可介入 =====
        if (as.getStatus() == 4 || as.getStatus() == 5) {
            throw new BusinessException(ErrorCode.AFTER_SALE_FINISHED);
        }

        // 2. 获取关联订单
        Order order = orderService.getById(as.getOrderId());
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);

        // ==== 阶段三 T1：仲裁入口兜底校验退款金额 ≤ 订单总额（防止仅靠申请时校验被绕过）====
        if (as.getAmount() != null && order.getTotalAmount() != null) {
            if (as.getAmount().compareTo(order.getTotalAmount()) > 0) {
                throw new BusinessException(ErrorCode.AFTER_SALE_AMOUNT_EXCEED,
                        "退款金额(" + as.getAmount() + ")超过订单总额(" + order.getTotalAmount() + ")");
            }
        }

        // ===== 3. status=4 仲裁通过（退款），status=5 驳回关闭 =====
        if (status == 4) {
            // 3.1 更新售后单状态为"仲裁完成"
            as.setStatus(4);
            as.setCompleteTime(LocalDateTime.now());
            as.setAdminReviewRemark(remark);

            // 3.2 更新订单状态为"已退款"（终态）
            order.setStatus(Order.STATUS_REFUNDED);
            order.setFinishTime(LocalDateTime.now());
            orderService.updateById(order);

            // 3.3 恢复库存（仅当为"退货退款"时）—— 原子 SQL，防并发丢更新
            if (as.getType() == 2) {
                LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(OrderItem::getOrderId, as.getOrderId());
                List<OrderItem> items = orderItemMapper.selectList(wrapper);
                for (OrderItem item : items) {
                    bookMapper.increaseStock(item.getBookId(), item.getQuantity());
                }
            }

            // ==== 阶段三 T1：不管哪种退款类型，都扣减销量（下单时 sales+sale，退款时必须对称扣减）====
            // 否则退货退款后销量虚高、榜单不准确
            LambdaQueryWrapper<OrderItem> salesWrapper = new LambdaQueryWrapper<>();
            salesWrapper.eq(OrderItem::getOrderId, as.getOrderId());
            List<OrderItem> salesItems = orderItemMapper.selectList(salesWrapper);
            for (OrderItem item : salesItems) {
                bookMapper.decreaseSales(item.getBookId(), item.getQuantity());
            }

            // 3.4 记录日志
            AfterSaleLog log = new AfterSaleLog();
            log.setAfterSaleId(afterSaleId);
            log.setOperator("admin");
            log.setOperatorId(0L);
            log.setAction("arbitrate_agree");
            log.setRemark("管理员仲裁通过，退款成功" + (remark != null ? "：" + remark : ""));
            afterSaleLogMapper.insert(log);

        } else if (status == 5) {
            // 4.1 驳回：售后单变为"已关闭"
            as.setStatus(5);
            as.setCompleteTime(LocalDateTime.now());
            as.setAdminReviewRemark(remark);

            // 4.2 订单状态变为"已关闭（售后驳回）"，允许重新申请
            order.setStatus(Order.STATUS_CLOSED);
            order.setUpdateTime(LocalDateTime.now());
            orderService.updateById(order);

            // 4.3 记录日志
            AfterSaleLog log = new AfterSaleLog();
            log.setAfterSaleId(afterSaleId);
            log.setOperator("admin");
            log.setOperatorId(0L);
            log.setAction("arbitrate_reject");
            log.setRemark("管理员仲裁驳回，订单关闭" + (remark != null ? "：" + remark : ""));
            afterSaleLogMapper.insert(log);

        } else {
            throw new BusinessException(ErrorCode.AFTER_SALE_ARBITRATE_INVALID);
        }

        // 5. 保存售后单变更
        this.updateById(as);
    }

    @Override
    public AfterSale getDetail(Long afterSaleId) {
        AfterSale as = this.getById(afterSaleId);
        if (as == null) throw new BusinessException(ErrorCode.AFTER_SALE_NOT_FOUND);
        return as;
    }
}
