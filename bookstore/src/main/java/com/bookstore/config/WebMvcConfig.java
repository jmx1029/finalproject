package com.bookstore.config;

import com.bookstore.interceptor.AuthorizationInterceptor;
import com.bookstore.interceptor.JwtInterceptor;
import com.bookstore.util.PathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private AuthorizationInterceptor authorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截器顺序：JWT 先验签存 userId/role，Authorization 再读 role 判路由
        registry.addInterceptor(jwtInterceptor)
                .order(1)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/books/**",
                        "/api/category/**",
                        "/uploads/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/favicon.ico"
                );

        // 角色鉴权：/admin/** → 管理员，/shop/** → 商家
        // 注意：AuthorizationInterceptor 默认覆盖 /admin/** /shop/**，而这些路径都经过了 JWT 拦截（不被放行），
        // 因此 JWT 一定会先执行、UserContext 一定已就绪。
        registry.addInterceptor(authorizationInterceptor)
                .order(2)
                .addPathPatterns("/admin/**", "/shop/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 统一使用 PathUtil 获取上传目录
        String uploadDir = PathUtil.getUploadDir();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir);
    }
}