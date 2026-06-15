package com.aihedgefund.service;

import com.aihedgefund.auth.JwtUtil;
import com.aihedgefund.auth.VerificationCodeStore;
import com.aihedgefund.auth.VerificationPurpose;
import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.mapper.UserMapper;
import com.aihedgefund.model.DO.UserDO;
import com.aihedgefund.model.req.LoginReq;
import com.aihedgefund.model.req.RegisterReq;
import com.aihedgefund.model.req.ResetPasswordReq;
import com.aihedgefund.model.resp.AuthResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证业务逻辑：注册、登录、发送验证码。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Value("${verify-code.ttl-seconds:300}")
    private int codeTtlSeconds;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final VerificationCodeStore codeStore;
    private final EmailService emailService;
    private final WalletService walletService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil, VerificationCodeStore codeStore, EmailService emailService,
            WalletService walletService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.codeStore = codeStore;
        this.emailService = emailService;
        this.walletService = walletService;
    }

    /**
     * 向指定邮箱发送注册验证码。
     *
     * @param email 目标邮箱
     */
    public void sendVerificationCode(String email) {
        log.info("发送注册验证码请求, email={}", email);
        String code = codeStore.generate(email, VerificationPurpose.REGISTER);
        int ttlMinutes = codeTtlSeconds / 60;
        emailService.sendVerificationCode(email, code, ttlMinutes);
        log.info("注册验证码已生成并发送, email={}", email);
    }

    /**
     * 注册新用户（需先通过邮箱验证码校验）。
     *
     * @param req 注册入参（含 code 字段）
     * @return 含 token 的认证结果
     */
    @Transactional
    public AuthResp register(RegisterReq req) {
        log.info("用户注册请求, email={}", req.getUsername());

        // 验证码校验
        if (!codeStore.verify(req.getUsername(), req.getCode(), VerificationPurpose.REGISTER)) {
            throw new BizException("验证码无效或已过期，请重新获取");
        }

        UserDO existing = userMapper.selectByUsername(req.getUsername());
        if (existing != null) {
            throw new BizException("该邮箱已注册");
        }

        UserDO user = new UserDO();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        String nickname = req.getNickname() != null && !req.getNickname().isBlank()
                ? req.getNickname() : req.getUsername();
        user.setNickname(nickname);
        userMapper.insert(user);
        // 注册完成后自动初始化钱包并赠送新人金币
        walletService.initWallet(user.getId());
        walletService.grantCoins(user.getId(), 38, "新用户注册赠送");

        log.info("用户注册成功, userId={}, email={}", user.getId(), user.getUsername());
        return buildAuthResp(user);
    }

    /**
     * 发送密码重置验证码。
     *
     * @param email 目标邮箱
     */
    public void sendPasswordResetCode(String email) {
        log.info("发送密码重置验证码请求, email={}", email);

        UserDO user = userMapper.selectByUsername(email);
        if (user == null) {
            throw new BizException("该邮箱未注册");
        }

        String code = codeStore.generate(email, VerificationPurpose.RESET_PASSWORD);
        int ttlMinutes = codeTtlSeconds / 60;
        emailService.sendPasswordResetCode(email, code, ttlMinutes);
        log.info("密码重置验证码已生成并发送, email={}", email);
    }

    /**
     * 重置密码（需先通过邮箱验证码校验）。
     *
     * @param req 重置密码入参
     */
    @Transactional
    public void resetPassword(ResetPasswordReq req) {
        log.info("重置密码请求, email={}", req.getEmail());

        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BizException("两次输入的密码不一致");
        }

        if (!codeStore.verify(req.getEmail(), req.getCode(), VerificationPurpose.RESET_PASSWORD)) {
            throw new BizException("验证码无效或已过期，请重新获取");
        }

        UserDO user = userMapper.selectByUsername(req.getEmail());
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }

        userMapper.updatePassword(user.getId(), passwordEncoder.encode(req.getNewPassword()));
        log.info("密码重置成功, userId={}, email={}", user.getId(), req.getEmail());
    }

    /**
     * 用户名密码登录。
     *
     * @param req 登录入参
     * @return 含 token 的认证结果
     */
    public AuthResp login(LoginReq req) {
        log.info("用户登录请求, username={}", req.getUsername());
        UserDO user = userMapper.selectByUsername(req.getUsername());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException("邮箱或密码错误");
        }

        log.info("用户登录成功, userId={}, username={}", user.getId(), user.getUsername());
        return buildAuthResp(user);
    }

    /**
     * 按 id 查询用户（供 /auth/me 使用）。
     */
    public UserDO getById(Long userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private AuthResp buildAuthResp(UserDO user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResp(token, user.getId(), user.getUsername(), user.getNickname());
    }
}
