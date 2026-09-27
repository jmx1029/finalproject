package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.common.ErrorCode;
import com.bookstore.dto.OrderSubmitDTO;
import com.bookstore.entity.Book;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;
import com.bookstore.entity.Shop;
import com.bookstore.entity.User;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.OrderItemMapper;
import com.bookstore.mapper.OrderMapper;
import com.bookstore.service.*;
import com.bookstore.util.UserContext;
import com.bookstore.vo.CartItemVO;
import com.bookstore.vo.OrderDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private CartService cartService;

    @Autowired
    private BookService bookService;   // ===== 新增注入 =====

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RecommenderService recommenderService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    // service/impl/OrderServiceImpl.java

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> submitOrder(OrderSubmitDTO dto) {
        Long userId = UserContext.getUserId();

        // 防重复提交：同一用户 5 秒内只允许一次下单请求
        String lockKey = "order:submit:lock:" + userId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
        if (acquired == null || !acquired) {
            throw new BusinessException(ErrorCode.ORDER_DUPLICATE_SUBMIT);
        }

        // 1. 获取选中的购物车商品
        List<CartItemVO> selectedItems = cartService.getSelectedItems(userId);
        if (selectedItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_SELECTION_EMPTY);
        }

        // 2. 获取所有选中图书的详细信息（包括 shopId, price, stock 等）
        List<Long> bookIds = selectedItems.stream().map(CartItemVO::getBookId).collect(Collectors.toList());
        List<Book> books = bookService.listByIds(bookIds);
        Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, Function.identity()));

        // 2.5 校验每本书是否仍在上架（status == 1），防止"加入购物车后下架"仍可下单
        for (Book book : books) {
            if (book.getStatus() == null || book.getStatus() != 1) {
                throw new BusinessException(ErrorCode.BOOK_NOT_FOUND, "图书「" + book.getTitle() + "」已下架，无法下单");
            }
        }
        // 3. 将购物车项按 shopId 分组（核心拆单逻辑）
        Map<Long, List<CartItemVO>> shopGroupMap = selectedItems.stream()
                .collect(Collectors.groupingBy(item -> {
                    Book book = bookMap.get(item.getBookId());
                    if (book == null) throw new BusinessException(ErrorCode.BOOK_NOT_FOUND, "图书不存在: " + item.getBookId());
                    // 如果图书没有 shopId（历史遗留或管理员创建），默认归到自营店（shopId=1）
                    Long shopId = book.getShopId();
                    return shopId != null ? shopId : 1L;
                }));

        // 4. 存储生成的订单ID（返回给前端）
        List<Long> orderIds = new ArrayList<>();

        // 5. 遍历每个店铺分组，生成独立的子订单
        for (Map.Entry<Long, List<CartItemVO>> entry : shopGroupMap.entrySet()) {
            Long shopId = entry.getKey();
            List<CartItemVO> shopItems = entry.getValue();

            // 5.1 计算本店铺小计
            BigDecimal shopTotal = BigDecimal.ZERO;
            for (CartItemVO item : shopItems) {
                Book book = bookMap.get(item.getBookId());
                shopTotal = shopTotal.add(book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }

            // 5.2 生成订单主表
            Order order = new Order();
            order.setUserId(userId);
            order.setShopId(shopId); // 绑定店铺
            order.setTotalAmount(shopTotal);
            order.setStatus(Order.STATUS_WAIT_PAY); // 待付款
            order.setReceiverName(dto.getReceiverName());
            order.setReceiverPhone(dto.getReceiverPhone());
            order.setReceiverAddress(dto.getReceiverAddress());
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            order.setIsDeleted(0);
            this.save(order); // MyBatis-Plus 自动回填 ID（雪花算法）
            orderIds.add(order.getId());

            // 5.3 生成订单明细 + 扣减库存（本店铺的商品）
            for (CartItemVO item : shopItems) {
                Book book = bookMap.get(item.getBookId());

                // 扣库存（SQL 带条件，防止超卖）
                int rows = bookMapper.decreaseStock(item.getBookId(), item.getQuantity());
                if (rows == 0) {
                    throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT,
                            "图书【" + book.getTitle() + "】库存不足，下单失败");
                }

                // 保存快照明细
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setBookId(item.getBookId());
                orderItem.setBookTitle(book.getTitle());
                orderItem.setBookCover(book.getCoverUrl());
                orderItem.setPrice(book.getPrice()); // 快照价格
                orderItem.setQuantity(item.getQuantity());
                orderItem.setCreateTime(LocalDateTime.now());
                orderItem.setUpdateTime(LocalDateTime.now());
                orderItem.setIsDeleted(0);
                orderItemMapper.insert(orderItem);
            }
        }

        // 6. 清空 Redis 购物车（所有选中项）
        for (CartItemVO item : selectedItems) {
            cartService.removeFromCart(userId, item.getBookId());
        }

        // 7. 清除推荐缓存
        recommenderService.clearRecommendCache(userId);

        // 8. 返回所有生成的订单ID列表
        return orderIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        // 归属权校验：只能支付自己的订单
        if (!order.getUserId().equals(UserContext.getUserId())) throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION);
        if (order.getStatus() != Order.STATUS_WAIT_PAY) throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);

        order.setStatus(Order.STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        if (order.getStatus() != Order.STATUS_PAID) throw new BusinessException(ErrorCode.ORDER_NOT_PAID);

        // ===== 服务端归属校验 =====
        // 管理员（role==1）可以发货任意订单；商家只能发自己店铺的订单；其他角色无权发货
        Integer role = UserContext.getRole();
        Long currentUserId = UserContext.getUserId();
        if (role == null || role != 1) {
            // 非管理员 → 校验是否是商家 + 订单归属自己店铺
            Long myShopId = shopService.getShopIdByUserId(currentUserId);
            if (myShopId == null) {
                throw new BusinessException(ErrorCode.SHOP_NOT_OWNED, "非商家无权发货");
            }
            if (order.getShopId() == null || !order.getShopId().equals(myShopId)) {
                throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION, "订单归属非本店铺，无权发货");
            }
        }

        order.setStatus(Order.STATUS_SHIPPED);  // 已发货
        order.setShipTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        // 归属校验：userId != null 时校验（买家/商家调用），null 时跳过（定时任务/管理员调用）
        if (userId != null && !order.getUserId().equals(userId)) throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION);
        // 状态校验：只能取消待付款订单（系统自动取消场景也是取消待付款订单）
        if (order.getStatus() != Order.STATUS_WAIT_PAY) throw new BusinessException(ErrorCode.ORDER_CANCEL_INVALID);

        // 1. 回补库存（下单时 decreaseStock 已扣减，取消时必须原子回补）
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId).eq(OrderItem::getIsDeleted, 0);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
        for (OrderItem item : items) {
            bookMapper.increaseStock(item.getBookId(), item.getQuantity());
        }

        // 2. 改订单状态为已取消
        order.setStatus(Order.STATUS_CANCELLED);
        order.setUpdateTime(LocalDateTime.now());
        this.updateById(order);

        log.info("订单已取消并回补库存: orderId={}, userId={}, 共回补 {} 件", orderId, userId, items.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishOrder(Long orderId, Long userId) {
        Order order = this.getById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        // 归属校验：只能确认自己的订单
        if (!order.getUserId().equals(userId)) throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION);
        // 状态校验：只有已发货才能确认收货
        if (order.getStatus() != Order.STATUS_SHIPPED) throw new BusinessException(ErrorCode.ORDER_FINISH_INVALID);

        order.setStatus(Order.STATUS_COMPLETED);
        order.setFinishTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        this.updateById(order);
    }


    @Override
    public OrderDetailVO getOrderDetail(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        // 归属权校验：只能查看自己的订单
        if (!order.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION);
        }

        // === 阶段五 T8：组装逻辑抽到 buildOrderDetailVO 公共方法 ===
        return buildOrderDetailVO(order);
    }

    /**
     * === 阶段五 T8：公共 VO 组装 ===
     * 把"查明细+查店铺+BeanUtils 拷属性"收敛到这里
     */
    @Override
    public OrderDetailVO buildOrderDetailVO(Order order) {
        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(order, vo);

        // 查店铺名称
        if (order.getShopId() != null) {
            Shop shop = shopService.getById(order.getShopId());
            vo.setShopName(shop != null ? shop.getName() : "未知店铺");
        } else {
            vo.setShopName("官方自营");
        }

        // 查订单明细
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, order.getId())
                .eq(OrderItem::getIsDeleted, 0);
        vo.setOrderItems(orderItemMapper.selectList(wrapper));

        return vo;
    }

    // ===== 新增：高级筛选分页查询 =====
    @Override
    public IPage<Order> pageQuery(Integer page, Integer size,
                                  Long orderId, Long userId,
                                  Integer status,
                                  LocalDateTime startTime, LocalDateTime endTime,
                                  BigDecimal minAmount, BigDecimal maxAmount) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getIsDeleted, 0);

        // 订单号精确查询
        if (orderId != null) {
            wrapper.eq(Order::getId, orderId);
        }
        // 用户ID精确查询
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        // 状态筛选
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        // 时间范围
        if (startTime != null) {
            wrapper.ge(Order::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Order::getCreateTime, endTime);
        }
        // 金额范围
        if (minAmount != null) {
            wrapper.ge(Order::getTotalAmount, minAmount);
        }
        if (maxAmount != null) {
            wrapper.le(Order::getTotalAmount, maxAmount);
        }

        wrapper.orderByDesc(Order::getCreateTime);
        return this.page(new Page<>(page, size), wrapper);
    }

    // ===== 新增：导出订单列表 =====
    @Override
    public List<Order> getOrdersForExport(Long orderId, Long userId,
                                          Integer status,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          BigDecimal minAmount, BigDecimal maxAmount) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getIsDeleted, 0);

        if (orderId != null) wrapper.eq(Order::getId, orderId);
        if (userId != null) wrapper.eq(Order::getUserId, userId);
        if (status != null) wrapper.eq(Order::getStatus, status);
        if (startTime != null) wrapper.ge(Order::getCreateTime, startTime);
        if (endTime != null) wrapper.le(Order::getCreateTime, endTime);
        if (minAmount != null) wrapper.ge(Order::getTotalAmount, minAmount);
        if (maxAmount != null) wrapper.le(Order::getTotalAmount, maxAmount);

        wrapper.orderByDesc(Order::getCreateTime);
        return this.list(wrapper);
    }

    // ===== 新增：统计报表数据 =====
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> result = new HashMap<>();

        // 1. 统计卡片
        Long totalOrders = orderMapper.countTotalOrders();
        BigDecimal totalSales = orderMapper.sumTotalSales();
        Long totalUsers = orderMapper.countDistinctUsers();
        Long totalBooks = orderMapper.countTotalBooks();

        result.put("totalOrders", totalOrders);
        result.put("totalSales", totalSales);
        result.put("totalUsers", totalUsers);
        result.put("totalBooks", totalBooks);

        // 2. 近7日销售趋势
        List<Map<String, Object>> trendData = orderMapper.getLast7DaysTrend();
        result.put("trendData", trendData);

        // 3. 热门分类销量
        List<Map<String, Object>> categorySales = orderMapper.getCategorySales();
        result.put("categorySales", categorySales);

        // 4. 热门商品 Top10
        List<Map<String, Object>> hotBooks = orderMapper.getHotBooksTop10();
        result.put("hotBooks", hotBooks);

        return result;
    }

    // ==================== 聊天订单交集 ====================

    @Override
    public List<Order> getChatOrdersAsBuyer(Long myUserId, Long receiverId) {
        // receiver 对象只查一次，后续分支复用
        User receiver = userService.getById(receiverId);
        if (receiver == null) return List.of();

        // 管理员：我下单的全部订单
        if (receiver.getRole() != null && receiver.getRole() == 1) {
            return list(new LambdaQueryWrapper<Order>()
                    .eq(Order::getUserId, myUserId)
                    .eq(Order::getIsDeleted, 0)
                    .orderByDesc(Order::getCreateTime));
        }

        // 商家：我下单、对方店铺的订单
        if (receiver.getIsShopOwner() != null && receiver.getIsShopOwner() == 1) {
            Long shopId = shopService.getShopIdByUserId(receiverId);
            if (shopId == null) return List.of();
            return list(new LambdaQueryWrapper<Order>()
                    .eq(Order::getUserId, myUserId)
                    .eq(Order::getShopId, shopId)
                    .eq(Order::getIsDeleted, 0)
                    .orderByDesc(Order::getCreateTime));
        }

        // 普通用户：防御性返回空
        return List.of();
    }

    @Override
    public List<Order> getChatOrdersAsShop(Long myUserId, Long receiverId) {
        Long myShopId = shopService.getShopIdByUserId(myUserId);
        if (myShopId == null) return List.of();

        // receiver 对象只查一次，后续分支复用
        User receiver = userService.getById(receiverId);
        if (receiver == null) return List.of();

        // 管理员：我店铺的全部订单
        if (receiver.getRole() != null && receiver.getRole() == 1) {
            return list(new LambdaQueryWrapper<Order>()
                    .eq(Order::getShopId, myShopId)
                    .eq(Order::getIsDeleted, 0)
                    .orderByDesc(Order::getCreateTime));
        }

        // 普通用户：我的店铺、对方买的订单
        return list(new LambdaQueryWrapper<Order>()
                .eq(Order::getShopId, myShopId)
                .eq(Order::getUserId, receiverId)
                .eq(Order::getIsDeleted, 0)
                .orderByDesc(Order::getCreateTime));
    }

    @Override
    public List<Order> getChatOrdersAsAdmin(Long receiverId) {
        // receiver 对象只查一次，后续分支复用
        User receiver = userService.getById(receiverId);
        if (receiver == null) return List.of();

        // receiver 是商家 → 该商家店铺全部订单
        if (receiver.getIsShopOwner() != null && receiver.getIsShopOwner() == 1) {
            Long shopId = shopService.getShopIdByUserId(receiverId);
            if (shopId == null) return List.of();
            return list(new LambdaQueryWrapper<Order>()
                    .eq(Order::getShopId, shopId)
                    .eq(Order::getIsDeleted, 0)
                    .orderByDesc(Order::getCreateTime));
        }

        // receiver 是普通用户或管理员 → 该 userId 下的全部订单
        return list(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, receiverId)
                .eq(Order::getIsDeleted, 0)
                .orderByDesc(Order::getCreateTime));
    }

}