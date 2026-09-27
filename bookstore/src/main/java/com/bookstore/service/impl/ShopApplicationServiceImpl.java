package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.common.ErrorCode;
import com.bookstore.dto.ShopApplyDTO;
import com.bookstore.entity.Shop;
import com.bookstore.entity.ShopApplication;
import com.bookstore.entity.User;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.ShopApplicationMapper;
import com.bookstore.service.ShopApplicationService;
import com.bookstore.service.ShopService;
import com.bookstore.service.UserService;
import com.bookstore.vo.ShopApplicationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class ShopApplicationServiceImpl extends ServiceImpl<ShopApplicationMapper, ShopApplication>
        implements ShopApplicationService {

    @Autowired
    private ShopService shopService;

    @Autowired
    private UserService userService;

    // ===== 构造器注入（推荐，消除字段注入警告） =====
    public ShopApplicationServiceImpl(ShopService shopService, UserService userService) {
        this.shopService = shopService;
        this.userService = userService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyShop(Long userId, ShopApplyDTO dto) {
        // 检查用户当前状态
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 如果用户已经是商家或审核中，不允许申请
        if (user.getIsShopOwner() == 1) {
            throw new BusinessException(ErrorCode.ALREADY_SHOP_OWNER);
        }
        if (user.getShopStatus() != null && user.getShopStatus() == 1) {
            throw new BusinessException(ErrorCode.SHOP_APPLY_PENDING);
        }
        // 如果用户状态为 2（已通过）或 0（未申请）或 3（已拒绝）可以申请，但已通过的情况上面已拦截，所以这里只需检查审核中

        // 检查是否已有审核中的申请（冗余检查）
        ShopApplication existing = this.getOne(new LambdaQueryWrapper<ShopApplication>()
                .eq(ShopApplication::getUserId, userId)
                .eq(ShopApplication::getStatus, 0)
                .eq(ShopApplication::getIsDeleted, 0));
        if (existing != null) {
            throw new BusinessException(ErrorCode.SHOP_APPLY_PENDING);
        }

        // 创建新申请
        ShopApplication application = new ShopApplication();
        application.setUserId(userId);
        application.setShopName(dto.getShopName());
        application.setDescription(dto.getDescription());
        application.setContactPerson(dto.getContactPerson());
        application.setContactPhone(dto.getContactPhone());
        application.setAddress(dto.getAddress());
        application.setIdCard(dto.getIdCard());
        application.setIdCardFront(dto.getIdCardFront());
        application.setIdCardBack(dto.getIdCardBack());
        application.setBusinessLicense(dto.getBusinessLicense());
        application.setStatus(0);
        this.save(application);

        // 更新用户 shop_status 为 1（审核中）
        user.setShopStatus(1);
        userService.updateById(user);
    }

    @Override
    public ShopApplication getLatestApplication(Long userId) {
        return this.getOne(new LambdaQueryWrapper<ShopApplication>()
                .eq(ShopApplication::getUserId, userId)
                .eq(ShopApplication::getIsDeleted, 0)
                .orderByDesc(ShopApplication::getCreateTime)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(Long applicationId, Integer status, String remark) {
        ShopApplication application = this.getById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.SHOP_APPLY_NOT_FOUND);
        }
        if (application.getStatus() != 0) {
            throw new BusinessException(ErrorCode.SHOP_APPLY_PROCESSED);
        }

        application.setStatus(status);
        application.setReviewRemark(remark);
        application.setUpdateTime(LocalDateTime.now());
        this.updateById(application);

        Long userId = application.getUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (status == 1) {
            // 审核通过：创建店铺，更新用户为商家
            Shop shop = shopService.createShop(
                    userId,
                    application.getShopName(),
                    application.getDescription(),
                    application.getContactPerson(),
                    application.getContactPhone(),
                    application.getAddress()
            );
            user.setShopId(shop.getId());
            user.setIsShopOwner(1);
            user.setShopStatus(2); // 已通过
            userService.updateById(user);
        } else if (status == 2) {
            // 审核拒绝：更新用户状态为已拒绝
            user.setShopStatus(3); // 已拒绝
            userService.updateById(user);
        }
    }

    @Override
    public IPage<ShopApplicationVO> getPendingApplications(Integer page, Integer size) {
        LambdaQueryWrapper<ShopApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopApplication::getStatus, 0)
                .eq(ShopApplication::getIsDeleted, 0)
                .orderByAsc(ShopApplication::getCreateTime);

        IPage<ShopApplication> applicationPage = this.page(new Page<>(page, size), wrapper);
        return convertToVOPage(applicationPage);
    }

    @Override
    public IPage<ShopApplicationVO> getAllApplications(Integer page, Integer size, Integer status) {
        LambdaQueryWrapper<ShopApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShopApplication::getIsDeleted, 0);
        if (status != null) {
            wrapper.eq(ShopApplication::getStatus, status);
        }
        wrapper.orderByDesc(ShopApplication::getCreateTime);

        IPage<ShopApplication> applicationPage = this.page(new Page<>(page, size), wrapper);
        return convertToVOPage(applicationPage);
    }

    private IPage<ShopApplicationVO> convertToVOPage(IPage<ShopApplication> page) {
        IPage<ShopApplicationVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(app -> {
            ShopApplicationVO vo = new ShopApplicationVO();
            BeanUtils.copyProperties(app, vo);

            // 获取用户信息
            User user = userService.getById(app.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }

            // 状态文本
            String[] statusTexts = {"待审核", "已通过", "已拒绝"};
            vo.setStatusText(statusTexts[app.getStatus()]);

            return vo;
        }).collect(Collectors.toList()));
        return result;
    }
}