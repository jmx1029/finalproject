package com.bookstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.dto.AfterSaleApplyDTO;
import com.bookstore.entity.AfterSale;

public interface AfterSaleService extends IService<AfterSale> {
    // 用户申请售后
    void apply(Long userId, AfterSaleApplyDTO dto);

    // 用户查询自己的售后单（分页）
    IPage<AfterSale> getUserAfterSales(Long userId, Integer page, Integer size, Integer status);

    // 商家查询自己店铺的售后单（分页）
    IPage<AfterSale> getShopAfterSales(Long shopId, Integer page, Integer size, Integer status);

    // 商家审核（通过/驳回）
    void shopReview(Long afterSaleId, Long shopId, Integer status, String remark);

    // 管理员仲裁
    void adminArbitrate(Long afterSaleId, Integer status, String remark);

    // 查询售后单详情
    AfterSale getDetail(Long afterSaleId);
}