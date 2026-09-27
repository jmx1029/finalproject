package com.bookstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.dto.ShopApplyDTO;
import com.bookstore.entity.ShopApplication;
import com.bookstore.vo.ShopApplicationVO;

public interface ShopApplicationService extends IService<ShopApplication> {
    /**
     * 用户申请成为商家
     */
    void applyShop(Long userId, ShopApplyDTO dto);

    /**
     * 获取用户的最新申请状态
     */
    ShopApplication getLatestApplication(Long userId);

    /**
     * 管理员审核商家申请
     */
    void reviewApplication(Long applicationId, Integer status, String remark);

    /**
     * 获取待审核列表（管理员）
     */
    IPage<ShopApplicationVO> getPendingApplications(Integer page, Integer size);

    /**
     * 获取所有申请列表（管理员）
     */
    IPage<ShopApplicationVO> getAllApplications(Integer page, Integer size, Integer status);
}