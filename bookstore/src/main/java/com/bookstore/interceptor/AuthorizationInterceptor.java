package com.bookstore.interceptor;

import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.service.ShopService;
import com.bookstore.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 统一角色鉴权拦截器
 * <p>
 * 职责：
 *   - /admin/**  必须管理员（role == 1）
 *   - /shop/**   必须商家（存在归属店铺）
 * <p>
 * 前置条件：JwtInterceptor 已先执行，UserContext 已包含 userId 和 role。
 * 注册顺序必须在 JwtInterceptor 之后。
 */
@Slf4j
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    @Autowired
    private ShopService shopService;

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        Long userId = UserContext.getUserId();
        Integer role = UserContext.getRole();

        // 1. /admin/** —— 必须 role == 1
        if (path.startsWith("/admin/")) {
            if (role == null || role != 1) {
                log.warn("鉴权拒绝 [userId={}, path={}] 非管理员访问 admin 路径", userId, path);
                writeError(response, 403, ErrorCode.ORDER_NO_PERMISSION.getCode(), "权限不足：该操作仅限管理员");
                return false;
            }
        }

        // 2. /shop/** —— 必须是商家（存在归属店铺）
        if (path.startsWith("/shop/")) {
            if (userId == null) {
                writeError(response, 401, 401, "未登录，请先登录");
                return false;
            }
            Long shopId = shopService.getShopIdByUserId(userId);
            if (shopId == null) {
                log.warn("鉴权拒绝 [userId={}, path={}] 非商家访问 shop 路径", userId, path);
                writeError(response, 403, ErrorCode.SHOP_NOT_OWNED.getCode(), "权限不足：您尚未拥有店铺");
                return false;
            }
        }

        return true;
    }

    private void writeError(HttpServletResponse response, int httpStatus, int code, String msg) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(om.writeValueAsString(Result.error(code, msg)));
    }
}
