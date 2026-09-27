package com.bookstore.exception;

import com.bookstore.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常 —— 用于捕获可预期的业务逻辑错误
 * 例如：库存不足、订单状态不允许、用户未登录等
 * 
 * 与系统异常的区别：
 *   - BusinessException：业务逻辑不满足条件，属于正常分支，应返回友好提示
 *   - RuntimeException/Exception：程序bug或不可预期错误，应返回"系统繁忙"
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 业务错误码
     */
    private final Integer code;

    /**
     * 从 ErrorCode 枚举构造 —— 最常用
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 从 ErrorCode 构造，但覆盖 message（用于需要动态消息的场景）
     */
    public BusinessException(ErrorCode errorCode, String msg) {
        super(msg);
        this.code = errorCode.getCode();
    }

    /**
     * 完整构造器 —— 手动指定 code 和 msg
     */
    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

    /**
     * 快捷构造器 —— 默认 code=400
     * 适用于不需要细分错误码的简单业务校验场景
     */
    public BusinessException(String msg) {
        super(msg);
        this.code = 400;
    }
}
