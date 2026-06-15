package com.aihedgefund.controller;

import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.mapper.UserMapper;
import com.aihedgefund.model.DO.UserDO;
import com.aihedgefund.model.req.GrantCoinsReq;
import com.aihedgefund.model.resp.WalletResp;
import com.aihedgefund.service.MessageBoardService;
import com.aihedgefund.service.WalletService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员接口（通过 X-Admin-Token 头鉴权，无需登录 JWT）。
 *
 * <p>调用示例（按用户 ID 发放）：
 * <pre>
 * curl -X POST http://localhost:8000/admin/coins/grant \
 *   -H "X-Admin-Token: mumu-admin-2024" \
 *   -H "Content-Type: application/json" \
 *   -d '{"userId":1,"amount":10,"reason":"初始金币"}'
 * </pre>
 * </p>
 *
 * <p>调用示例（按注册邮箱发放）：
 * <pre>
 * curl -X POST http://localhost:8000/admin/coins/grant \
 *   -H "X-Admin-Token: mumu-admin-2024" \
 *   -H "Content-Type: application/json" \
 *   -d '{"email":"3433256865@qq.com","amount":10,"reason":"初始金币"}'
 * </pre>
 * </p>
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final WalletService walletService;
    private final UserMapper userMapper;
    private final MessageBoardService messageBoardService;

    public AdminController(WalletService walletService, UserMapper userMapper,
            MessageBoardService messageBoardService) {
        this.walletService = walletService;
        this.userMapper = userMapper;
        this.messageBoardService = messageBoardService;
    }

    /**
     * 向指定用户发放金币，支持按用户 ID 或注册邮箱定位用户。
     */
    @PostMapping("/coins/grant")
    public Map<String, Object> grantCoins(@RequestBody @Valid GrantCoinsReq req) {
        log.info("管理员发放金币请求, userId={}, email={}, amount={}",
                req.getUserId(), req.getEmail(), req.getAmount());

        UserDO user = resolveUser(req);
        String reason = req.getReason() != null ? req.getReason() : "管理员发放";
        WalletResp wallet = walletService.grantCoins(user.getId(), req.getAmount(), reason);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("amount", req.getAmount());
        result.put("balanceAfter", wallet.getBalance());
        result.put("message", "发放成功");
        return result;
    }

    /**
     * 根据请求中的 userId 或 email 定位用户，两者都未提供或用户不存在时抛出异常。
     */
    private UserDO resolveUser(GrantCoinsReq req) {
        if (req.getUserId() != null) {
            UserDO user = userMapper.selectById(req.getUserId());
            if (user == null) {
                throw new BizException(404, "用户不存在: " + req.getUserId());
            }
            return user;
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            UserDO user = userMapper.selectByUsername(req.getEmail().trim());
            if (user == null) {
                throw new BizException(404, "用户不存在: " + req.getEmail());
            }
            return user;
        }
        throw new BizException(400, "userId 和 email 不能同时为空");
    }

    /**
     * 查询所有用户的钱包余额列表（用于运营管理）。
     */
    @GetMapping("/users/balances")
    public List<Map<String, Object>> listUserBalances() {
        List<UserDO> users = userMapper.selectAll();
        return users.stream().map(user -> {
            WalletResp wallet = walletService.getBalance(user.getId());
            Map<String, Object> row = new HashMap<>();
            row.put("userId", user.getId());
            row.put("username", user.getUsername());
            row.put("nickname", user.getNickname());
            row.put("balance", wallet.getBalance());
            return row;
        }).collect(Collectors.toList());
    }

    /**
     * 管理员删除任意留言（逻辑删除）。
     */
    @DeleteMapping("/message-board/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        log.info("管理员删除留言, id={}", id);
        messageBoardService.deleteByAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
