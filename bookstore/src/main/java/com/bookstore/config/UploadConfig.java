package com.bookstore.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * === 阶段五 T5：System.out.println 全部替换为 log.debug/info ===
 * 这些打印在生产环境不应出现；调试时可以临时开 DEBUG 级别看
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "bookstore.upload")
public class UploadConfig {

    private String path = "uploads/";

    public void setPath(String path) {
        this.path = path;
    }

    @Bean(name = "uploadAbsolutePath")
    public String uploadAbsolutePath() {
        ApplicationHome home = new ApplicationHome(UploadConfig.class);
        File rootDir = home.getDir();

        if (rootDir != null) {
            String absPath = rootDir.getAbsolutePath();
            log.debug("ApplicationHome 返回: {}", absPath);

            if (absPath.contains("target" + File.separator + "classes") ||
                    absPath.contains("target\\classes")) {
                rootDir = rootDir.getParentFile().getParentFile();
                log.debug("检测到 target/classes，向上取到: {}", rootDir);
            } else if (absPath.contains("target")) {
                rootDir = rootDir.getParentFile();
                log.debug("检测到 target，向上取到: {}", rootDir);
            }
        }

        if (rootDir == null) {
            rootDir = new File(System.getProperty("user.dir"));
            log.debug("使用 user.dir: {}", rootDir);
        }

        Path uploadPath = Paths.get(rootDir.getAbsolutePath(), path).normalize();
        File uploadDir = uploadPath.toFile();

        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String result = uploadDir.getAbsolutePath();
        log.info("最终上传目录: {}", result);
        return result;
    }
}
