package com.aihedgefund.schedule;

import com.aihedgefund.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日定时任务：零点自动补齐用户金币。
 *
 * 规则：余额 < 3 的用户，补齐到 3 枚。
 * 阈值通过 wallet.daily-topup-threshold 配置，默认 3。
 */
@Component
public class DailyTopupTask {

    private static final Logger log = LoggerFactory.getLogger(DailyTopupTask.class);

    private final WalletService walletService;

    public DailyTopupTask(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * 每天 0:00 自动执行
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyTopup() {
        log.info("=== 每日金币补齐任务开始 ===");
        try {
            int count = walletService.dailyTopup();
            log.info("=== 每日金币补齐任务完成，补齐 {} 位用户 ===", count);
        } catch (Exception e) {
            log.error("每日金币补齐任务异常", e);
        }
    }
}
