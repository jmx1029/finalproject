package com.bookstore.controller;

import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.exception.BusinessException;
import com.bookstore.util.PathUtil;
import com.bookstore.util.UserContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/file")
public class FileController {

    /** 文件大小上限：10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    /** 允许的图片类型 */
    private static final Set<String> ALLOWED_IMAGE_MIME = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    ));

    /** 允许的文档类型 */
    private static final Set<String> ALLOWED_DOC_MIME = new HashSet<>(Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ));

    /** 允许的安全文件后缀 */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
            ".pdf", ".doc", ".docx"
    ));

    // ===== 业务类型 → 子目录映射规则 =====
    private static final Map<String, String> TYPE_SUB_DIR_MAP = new HashMap<>();
    static {
        TYPE_SUB_DIR_MAP.put("avatar", "avatar/");
        TYPE_SUB_DIR_MAP.put("shop_logo", "shop/{userId}/logo/");
        TYPE_SUB_DIR_MAP.put("shop_book", "shop/{userId}/books/");
        TYPE_SUB_DIR_MAP.put("common", "common/");
        TYPE_SUB_DIR_MAP.put("admin_book", "books/");
        TYPE_SUB_DIR_MAP.put("carousel", "carousel/");
        TYPE_SUB_DIR_MAP.put("home", "home/");
        TYPE_SUB_DIR_MAP.put("message", "message/");
    }

    @PostMapping("/upload")
    public Result<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "common") String type) throws IOException {

        // 1. 空文件校验
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        // 2. 文件大小校验（防止超大文件 + DoS）
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        // 3. 文件后缀白名单校验（防止上传 .jsp/.html/.exe 等危险文件）
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!ext.isEmpty() && !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_INVALID);
        }

        // 4. MIME 类型校验（防止伪造后缀绕过）
        String contentType = file.getContentType();
        if (contentType != null) {
            boolean isAllowed = ALLOWED_IMAGE_MIME.contains(contentType) || ALLOWED_DOC_MIME.contains(contentType);
            if (!isAllowed) {
                throw new BusinessException(ErrorCode.FILE_TYPE_INVALID);
            }
        }

        // 5. 路径穿越防护
        String originalNameSafe = originalName != null ? originalName.replace("..", "_").replace("/", "_").replace("\\", "_") : "file";

        // 6. 获取当前用户ID（用于隔离目录）
        Long userId = UserContext.getUserId();
        if (userId == null && ("shop_logo".equals(type) || "shop_book".equals(type))) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 7. 根据 type 获取子目录模板，并替换 {userId}
        String subDirTemplate = TYPE_SUB_DIR_MAP.getOrDefault(type, "common/");
        String subDir = subDirTemplate.replace("{userId}", String.valueOf(userId));

        // 8. 调用 PathUtil 创建目录并获取绝对路径
        String targetDir = PathUtil.getSubDir(subDir);

        // 9. 生成唯一文件名（防重名）
        String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        String fullPath = targetDir + filename;

        // 10. 保存文件
        File dest = new File(fullPath);
        file.transferTo(dest);

        // 11. 返回相对访问路径
        String accessPath = "/uploads/" + subDir + filename;
        return Result.success(accessPath);
    }

    @GetMapping("/types")
    public Result<Map<String, String>> getSupportedTypes() {
        return Result.success(TYPE_SUB_DIR_MAP);
    }
}
