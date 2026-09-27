package com.bookstore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 路径工具类
 * <p>
 * 核心功能：跨环境稳定解析文件路径
 * <p>
 * 支持场景：
 * 1. IDEA 直接运行 Main 类 → 返回项目根目录
 * 2. mvn spring-boot:run → 返回项目根目录
 * 3. java -jar xxx.jar → 返回 jar 包所在目录
 * <p>
 * 使用方式：
 * String uploadDir = PathUtil.resolvePath("./uploads/");
 */
@Component
public class PathUtil {

    private static final Logger log = LoggerFactory.getLogger(PathUtil.class);

    private static final String DEFAULT_UPLOAD_PATH = "./uploads/";

    /**
     * 获取应用根目录（跨环境稳定）
     * <p>
     * 原理：使用 Spring Boot 官方提供的 ApplicationHome 类，
     * 它能正确识别 IDEA 开发环境、mvn 运行、jar 包运行等场景。
     *
     * @return 应用根目录的 File 对象
     */
    public static File getAppRootDir() {
        // 1. 优先使用 ApplicationHome（Spring Boot 官方方案）
        ApplicationHome home = new ApplicationHome(PathUtil.class);
        File rootDir = home.getDir();

        // 2. 如果获取失败，使用 user.dir 兜底
        if (rootDir == null) {
            rootDir = new File(System.getProperty("user.dir"));
        }

        // 3. 如果 rootDir 指向 target/classes 或 target 目录，向上取到项目根目录
        String absPath = rootDir.getAbsolutePath();
        if (absPath.contains("target" + File.separator + "classes") ||
                absPath.contains("target\\classes")) {
            // 向上走两步：target/classes → target → 项目根目录
            rootDir = rootDir.getParentFile().getParentFile();
        } else if (absPath.contains("target")) {
            // 如果是 target 目录，取父目录
            rootDir = rootDir.getParentFile();
        }

        // 4. 最终兜底
        if (rootDir == null) {
            rootDir = new File(System.getProperty("user.dir"));
        }

        return rootDir;
    }

    /**
     * 解析相对路径为绝对路径
     * <p>
     * 基于应用根目录解析，支持以下格式：
     * - "./uploads/" → E:/project/uploads/
     * - "uploads/"   → E:/project/uploads/
     * - "uploads"    → E:/project/uploads/
     *
     * @param relativePath 相对路径（如 "./uploads/"）
     * @return 规范化的绝对路径（如 "E:/project/uploads/"）
     */
    public static String resolvePath(String relativePath) {
        // 1. 如果已经是绝对路径，直接返回
        Path path = Paths.get(relativePath);
        if (path.isAbsolute()) {
            return normalizePath(path.toString());
        }

        // 2. 基于应用根目录解析
        File rootDir = getAppRootDir();
        File resolvedFile = new File(rootDir, relativePath);

        // 3. 创建目录（如果不存在）
        if (!resolvedFile.exists()) {
            boolean created = resolvedFile.mkdirs();
            if (created) {
                log.debug("创建目录: {}", resolvedFile.getAbsolutePath());
            }
        }

        return normalizePath(resolvedFile.getAbsolutePath());
    }

    /**
     * 解析默认上传目录（./uploads/）
     *
     * @return 上传目录的绝对路径
     */
    public static String getUploadDir() {
        return resolvePath(DEFAULT_UPLOAD_PATH);
    }

    /**
     * 获取子目录路径（如 ./uploads/avatar/）
     *
     * @param subPath 子路径（如 "avatar/"）
     * @return 子目录的绝对路径
     */
    public static String getSubDir(String subPath) {
        String base = resolvePath(DEFAULT_UPLOAD_PATH);
        String fullPath = base + subPath;
        File dir = new File(fullPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return normalizePath(fullPath);
    }

    /**
     * 规范化路径（统一分隔符 + 确保末尾斜杠）
     *
     * @param path 原始路径
     * @return 规范化后的路径
     */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        // 统一使用 / 分隔符（跨平台）
        String normalized = path.replace("\\", "/");
        // 如果路径不是以 / 结尾，添加 /
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    /**
     * 拼接路径
     *
     * @param base  基础路径
     * @param child 子路径
     * @return 拼接后的路径
     */
    public static String join(String base, String child) {
        String baseNorm = normalizePath(base);
        String childNorm = child.replace("\\", "/");
        if (childNorm.startsWith("/")) {
            childNorm = childNorm.substring(1);
        }
        return baseNorm + childNorm;
    }

    /**
     * 判断路径是否有效（文件或目录存在）
     */
    public static boolean exists(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return new File(path).exists();
    }
}