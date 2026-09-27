package com.bookstore.exception;

import com.bookstore.common.Result;
import com.bookstore.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 —— 预期内的业务逻辑错误
     * 携带 userId 和请求路径，方便定位问题；不打印堆栈（避免日志噪音）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String path = request.getRequestURI();
        log.warn("业务异常 [userId={}, path={}] code={}, msg={}", userId, path, e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常 —— @Valid 校验失败
     * 拼接所有字段错误信息，返回 code=400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        Long userId = UserContext.getUserId();
        String path = request.getRequestURI();
        log.warn("参数校验失败 [userId={}, path={}] {}", userId, path, msg);
        return Result.error(400, msg);
    }

    /**
     * === 阶段四 T2：请求体 JSON 解析异常 ===
     * 场景：body 不是合法 JSON、字段类型不匹配（如前端传字符串给 Integer 字段）
     * 对外返回 400，不暴露内部类名/字段路径
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String path = request.getRequestURI();
        log.warn("请求体 JSON 解析异常 [userId={}, path={}]: {}", userId, path, e.getMessage());
        return Result.error(400, "请求体格式不正确");
    }

    /**
     * === 阶段四 T2：路径参数/查询参数类型绑定异常 ===
     * 场景：/book/{id} 传 "abc"、query 里 status="xxx"
     * 对外返回 400，不暴露 Converter 内部细节
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String path = request.getRequestURI();
        log.warn("参数类型不匹配 [userId={}, path={}] param={}, value={}",
                userId, path, e.getName(), e.getValue());
        return Result.error(400, "参数类型不正确：" + e.getName());
    }

    /**
     * === 阶段四 T2：必填 query/path 参数缺失 ===
     * 场景：@RequestParam 标 required=true 但前端没传
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String path = request.getRequestURI();
        log.warn("缺少必填参数 [userId={}, path={}] param={}", userId, path, e.getParameterName());
        return Result.error(400, "缺少必填参数：" + e.getParameterName());
    }

    /**
     * 静态资源 404 —— 浏览器自动请求 /favicon.ico 等固定资源时触发
     * 不打印 error 堆栈，避免日志噪音
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.debug("静态资源不存在: {}", path);
        return Result.error(404, "资源不存在");
    }

    /**
     * === 阶段四 T1：运行时异常 —— 捕获尚未改造为 BusinessException 的 RuntimeException ===
     * 关键：对外返回固定文案，不再把内部 e.getMessage()（SQL 报错、类名、路径等）暴露给前端
     * 详细堆栈只进日志，方便开发定位
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String path = request.getRequestURI();
        log.error("运行时异常 [userId={}, path={}]", userId, path, e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }

    /**
     * 系统异常 —— 兜底处理，隐藏内部错误细节
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String path = request.getRequestURI();
        log.error("系统异常 [userId={}, path={}]", userId, path, e);
        return Result.error(500, "系统繁忙，请稍后再试");
    }
}
