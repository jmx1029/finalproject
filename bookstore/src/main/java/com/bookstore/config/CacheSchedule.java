package com.bookstore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时清理缓存任务
 * <p>
 * 虽然 CacheConfig 已经给每个缓存空间配置了 TTL（home=10min, ranking=5min 等），
 * 但 Redis 的惰性删除 + 定期删除机制可能导致过期数据残留，
 * 这里主动定时清理热点缓存空间，确保数据新鲜度。
 */
@Component
public class CacheSchedule {

    private static final Logger log = LoggerFactory.getLogger(CacheSchedule.class);

    @Autowired
    private CacheManager cacheManager;

    /**
     * 每 5 分钟清理 ranking 缓存（热榜变化快）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void clearRankingCache() {
        clearCache("ranking");
    }

    /**
     * 每 10 分钟清理 home 缓存（首页数据）
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void clearHomeCache() {
        clearCache("home");
    }

    /**
     * 每 30 分钟清理 book 推荐缓存 + shop 缓存
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void clearBookAndShopCache() {
        clearCache("book");
        clearCache("shop");
    }

    private void clearCache(String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("[Cache] 已清理缓存空间: {}", cacheName);
            }
        } catch (Exception e) {
            log.warn("[Cache] 清理缓存 {} 失败: {}", cacheName, e.getMessage());
        }
    }
}
