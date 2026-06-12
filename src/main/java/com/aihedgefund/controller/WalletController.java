package com.aihedgefund.controller;

import com.aihedgefund.auth.AuthUser;
import com.aihedgefund.auth.UserContext;
import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.model.resp.CoinTransactionResp;
import com.aihedgefund.model.resp.WalletResp;
import com.aihedgefund.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户钱包接口（需登录）。
 */
@RestController
@RequestMapping("/wallet")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * 查询当前用户余额。
     */
    @GetMapping("/balance")
    public WalletResp balance() {
        Long userId = currentUserId();
        log.debug("查询钱包余额, userId={}", userId);
        return walletService.getBalance(userId);
    }

    /**
     * 分页查询当前用户金币流水。
     *
     * @param pageNum  页码（默认 1）
     * @param pageSize 每页条数（默认 20，最大 50）
     */
    @GetMapping("/transactions")
    public Map<String, Object> transactions(
            @RequestParam(defaultValue = "1")  int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (pageSize > 50) {
            pageSize = 50;
        }
        Long userId = currentUserId();
        List<CoinTransactionResp> list = walletService.listTransactions(userId, pageNum, pageSize);
        int total = walletService.countTransactions(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    private Long currentUserId() {
        AuthUser user = UserContext.get();
        if (user == null) {
            throw new BizException(401, "未登录");
        }
        return user.getUserId();
    }
}
