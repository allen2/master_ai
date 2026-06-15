package com.aihedgefund.service;

import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.mapper.CoinTransactionMapper;
import com.aihedgefund.mapper.UserWalletMapper;
import com.aihedgefund.model.DO.CoinTransactionDO;
import com.aihedgefund.model.DO.UserWalletDO;
import com.aihedgefund.model.resp.CoinTransactionResp;
import com.aihedgefund.model.resp.WalletResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 钱包业务逻辑：余额查询、金币发放（管理员）、金币消耗（分析）、每日补齐。
 */
@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private static final String TYPE_GRANT  = "GRANT";
    private static final String TYPE_DEDUCT = "DEDUCT";

    /** 每日补齐阈值：余额低于此值时补齐到该值 */
    @Value("${wallet.daily-topup-threshold:3}")
    private int dailyTopupThreshold;

    private final UserWalletMapper walletMapper;
    private final CoinTransactionMapper txMapper;

    public WalletService(UserWalletMapper walletMapper, CoinTransactionMapper txMapper) {
        this.walletMapper = walletMapper;
        this.txMapper = txMapper;
    }

    /**
     * 注册用户时初始化钱包（余额=0）。
     *
     * @param userId 新用户 ID
     */
    @Transactional
    public void initWallet(Long userId) {
        UserWalletDO wallet = new UserWalletDO();
        wallet.setUserId(userId);
        wallet.setBalance(0);
        walletMapper.insert(wallet);
        log.info("钱包初始化成功, userId={}", userId);
    }

    /**
     * 查询指定用户的余额。
     *
     * @param userId 用户 ID
     * @return 钱包余额响应
     */
    public WalletResp getBalance(Long userId) {
        UserWalletDO wallet = walletMapper.selectByUserId(userId);
        int balance = wallet != null ? wallet.getBalance() : 0;
        return new WalletResp(userId, balance);
    }

    /**
     * 管理员向指定用户发放金币。
     *
     * @param userId 目标用户 ID
     * @param amount 发放数量（正整数）
     * @param reason 发放原因
     * @return 操作后的余额
     */
    @Transactional
    public WalletResp grantCoins(Long userId, int amount, String reason) {
        log.info("管理员发放金币, userId={}, amount={}, reason={}", userId, amount, reason);
        UserWalletDO wallet = walletMapper.selectByUserId(userId);
        if (wallet == null) {
            // 用户没有钱包则自动创建
            initWallet(userId);
        }
        walletMapper.addBalance(userId, amount);

        UserWalletDO updated = walletMapper.selectByUserId(userId);
        int balanceAfter = updated != null ? updated.getBalance() : amount;

        CoinTransactionDO tx = buildTx(userId, amount, TYPE_GRANT, reason, balanceAfter);
        txMapper.insert(tx);

        log.info("金币发放完成, userId={}, amount={}, balanceAfter={}", userId, amount, balanceAfter);
        return new WalletResp(userId, balanceAfter);
    }

    /**
     * 分析前扣减 1 枚金币。余额不足时抛出 BizException(402)。
     *
     * @param userId 用户 ID
     */
    @Transactional
    public void deductOneForAnalysis(Long userId) {
        int affected = walletMapper.deductOne(userId);
        if (affected == 0) {
            log.warn("金币不足，拒绝分析, userId={}", userId);
            throw new BizException(402, "金币不足，请联系管理员充值");
        }

        UserWalletDO updated = walletMapper.selectByUserId(userId);
        int balanceAfter = updated != null ? updated.getBalance() : 0;

        CoinTransactionDO tx = buildTx(userId, -1, TYPE_DEDUCT, "AI 分析消耗", balanceAfter);
        txMapper.insert(tx);
        log.info("扣减金币成功, userId={}, balanceAfter={}", userId, balanceAfter);
    }

    /**
     * 分页查询金币流水（最新在前）。
     *
     * @param userId   用户 ID
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 流水列表
     */
    public List<CoinTransactionResp> listTransactions(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return txMapper.selectByUserId(userId, offset, pageSize).stream()
                .map(this::convertTx)
                .collect(Collectors.toList());
    }

    public int countTransactions(Long userId) {
        return txMapper.countByUserId(userId);
    }

    private CoinTransactionDO buildTx(Long userId, int amount, String type, String reason, int balanceAfter) {
        CoinTransactionDO tx = new CoinTransactionDO();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setReason(reason);
        tx.setBalanceAfter(balanceAfter);
        return tx;
    }

    private CoinTransactionResp convertTx(CoinTransactionDO tx) {
        CoinTransactionResp resp = new CoinTransactionResp();
        resp.setId(tx.getId());
        resp.setAmount(tx.getAmount());
        resp.setType(tx.getType());
        resp.setReason(tx.getReason());
        resp.setBalanceAfter(tx.getBalanceAfter());
        resp.setCreatedAt(tx.getCreatedAt());
        return resp;
    }

    /**
     * 每日补齐：将余额低于阈值的用户补齐到阈值。
     * 由定时任务每天凌晨自动调用，也可手动触发。
     *
     * @return 补齐的用户数量
     */
    @Transactional
    public int dailyTopup() {
        List<UserWalletDO> wallets = walletMapper.selectBalanceBelow(dailyTopupThreshold);
        if (wallets.isEmpty()) {
            log.info("每日补齐：无需补齐的用户");
            return 0;
        }

        int count = 0;
        for (UserWalletDO wallet : wallets) {
            int shortfall = dailyTopupThreshold - wallet.getBalance();
            walletMapper.addBalance(wallet.getUserId(), shortfall);

            UserWalletDO updated = walletMapper.selectByUserId(wallet.getUserId());
            int balanceAfter = updated != null ? updated.getBalance() : dailyTopupThreshold;

            CoinTransactionDO tx = buildTx(wallet.getUserId(), shortfall, TYPE_GRANT,
                    "每日补齐（" + wallet.getBalance() + "→" + dailyTopupThreshold + "）", balanceAfter);
            txMapper.insert(tx);

            log.info("每日补齐: userId={}, {}→{}, +{}", wallet.getUserId(), wallet.getBalance(), balanceAfter, shortfall);
            count++;
        }
        log.info("每日补齐完成，共补齐 {} 位用户", count);
        return count;
    }
}
