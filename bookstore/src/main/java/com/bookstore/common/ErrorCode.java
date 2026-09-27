package com.bookstore.common;

import lombok.Getter;

/**
 * 全局业务错误码枚举
 * 
 * 编码规则：模块号 + 序号（4位）
 *   1xxx 用户模块    登录/注册/信息修改
 *   2xxx 店铺模块    申请/审核/店铺权限
 *   3xxx 商品模块    图书CRUD/库存
 *   4xxx 购物车模块  购物车操作
 *   5xxx 订单模块    订单创建/支付/发货
 *   6xxx 售后模块    售后申请/审核/仲裁
 *   7xxx 评论模块    评论/回复/权限
 *   8xxx 消息模块    会话/消息
 */
@Getter
public enum ErrorCode {

    // ========== 用户模块 1xxx ==========
    USERNAME_NOT_FOUND(1001, "用户名不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    USER_FROZEN(1003, "账号已被冻结，请联系管理员"),
    USERNAME_ALREADY_EXISTS(1004, "用户名已被注册"),
    USER_NOT_FOUND(1005, "用户不存在"),
    OLD_PASSWORD_ERROR(1006, "原密码错误"),
    FILE_EMPTY(1007, "文件不能为空"),
    AVATAR_UPLOAD_FAIL(1008, "头像上传失败"),
    FILE_TYPE_INVALID(1009, "文件类型不支持"),
    FILE_TOO_LARGE(1010, "文件大小不能超过 10MB"),

    // ========== 店铺模块 2xxx ==========
    SHOP_NOT_OWNED(2001, "您尚未拥有店铺"),
    ALREADY_SHOP_OWNER(2002, "您已经是商家了"),
    SHOP_APPLY_PENDING(2003, "您已有审核中的申请，请耐心等待"),
    SHOP_APPLY_NOT_FOUND(2004, "申请不存在"),
    SHOP_APPLY_PROCESSED(2005, "该申请已处理"),
    NOT_SHOP_OWNER(2006, "您还不是商家，请先申请成为商家"),
    SHOP_COMMENT_RATING_INVALID(2007, "评分必须在 1-10 之间"),
    SHOP_COMMENT_ALREADY_EXISTS(2008, "您已评价过该店铺"),
    SHOP_COMMENT_NOT_FOUND(2009, "店铺评价不存在"),
    SHOP_COMMENT_NO_PERMISSION(2010, "无权操作此评价"),

    // ========== 商品模块 3xxx ==========
    BOOK_NOT_FOUND(3001, "图书不存在"),
    BOOK_NO_PERMISSION_MODIFY(3002, "无权修改此图书"),
    BOOK_NO_PERMISSION_DELETE(3003, "无权删除此图书"),
    BOOK_NO_PERMISSION_OPERATE(3004, "无权操作此图书"),
    STOCK_INSUFFICIENT(3005, "库存不足"),

    // ========== 购物车模块 4xxx ==========
    CART_ITEM_NOT_FOUND(4001, "购物车中不存在该商品"),
    CART_SELECTION_EMPTY(4002, "请先选择要结算的商品"),

    // ========== 订单模块 5xxx ==========
    ORDER_NOT_FOUND(5001, "订单不存在"),
    ORDER_NO_PERMISSION(5002, "无权操作此订单"),
    ORDER_STATUS_ERROR(5003, "订单状态不正确"),
    ORDER_NOT_PAID(5004, "订单未支付，不能发货"),
    ORDER_CANCEL_INVALID(5005, "只能取消待付款的订单"),
    ORDER_FINISH_INVALID(5006, "只能确认已发货的订单"),
    ORDER_SHIP_INVALID(5007, "只有已支付的订单才能发货"),
    ORDER_DUPLICATE_SUBMIT(5008, "请勿重复提交，请稍后再试"),

    // ========== 售后模块 6xxx ==========
    AFTER_SALE_NOT_FOUND(6001, "售后单不存在"),
    AFTER_SALE_NO_PERMISSION(6002, "无权操作此售后单"),
    AFTER_SALE_STATUS_INVALID(6003, "当前订单状态不可申请售后"),
    AFTER_SALE_TIMEOUT(6004, "订单已完成超过15天，无法申请售后"),
    AFTER_SALE_IN_PROGRESS(6005, "该订单已有进行中的售后申请"),
    AFTER_SALE_ALREADY_PROCESSED(6006, "该售后单已处理，不可重复操作"),
    AFTER_SALE_REVIEW_STATUS_INVALID(6007, "审核状态不正确"),
    AFTER_SALE_FINISHED(6008, "该售后单已完结，无法仲裁"),
    AFTER_SALE_ARBITRATE_INVALID(6009, "仲裁状态不正确，请使用4（通过）或5（驳回）"),

    // ========== 评论模块 7xxx ==========
    COMMENT_NOT_FOUND(7001, "评论不存在"),
    COMMENT_NO_PERMISSION_DELETE(7002, "无权删除此评论"),
    COMMENT_NO_PERMISSION_REPLY(7003, "无权回复此评价"),
    EVALUATION_NOT_FOUND(7004, "评价不存在"),
    // === 阶段五 T7：新增专门错误码，不再复用 COMMENT_NOT_FOUND ===
    COMMENT_PURCHASE_REQUIRED(7005, "您还没有购买过这本书，无法评价"),
    COMMENT_DUPLICATE(7006, "您已经评价过这本书了，不能重复评价"),

    // ========== 消息模块 8xxx ==========
    RECEIVER_ID_FORMAT_ERROR(8001, "接收方ID格式错误"),
    SESSION_NOT_FOUND(8002, "会话不存在或无权访问"),
    MESSAGE_NOT_FOUND(8003, "消息不存在"),
    MESSAGE_CONTENT_REQUIRED(8004, "文本消息内容不能为空"),

    // ========== 通用 ==========
    ORDER_ID_FORMAT_ERROR(9001, "订单ID格式不正确"),
    SHOP_ID_NULL(9002, "店铺信息异常"),
    AFTER_SALE_AMOUNT_EXCEED(9003, "退款金额不能超过订单总额");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
