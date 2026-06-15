package com.aihedgefund.controller;

import com.aihedgefund.auth.AuthUser;
import com.aihedgefund.auth.UserContext;
import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.model.req.MessageBoardReq;
import com.aihedgefund.model.resp.MessageBoardResp;
import com.aihedgefund.service.MessageBoardService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 留言板接口（需登录）：所有登录用户可发表、查看、点赞留言，删除仅限本人留言。
 */
@RestController
@RequestMapping("/message-board")
public class MessageBoardController {

    private static final Logger log = LoggerFactory.getLogger(MessageBoardController.class);

    private final MessageBoardService messageBoardService;

    public MessageBoardController(MessageBoardService messageBoardService) {
        this.messageBoardService = messageBoardService;
    }

    /**
     * 分页查询留言列表（按发表时间倒序）。
     *
     * @param pageNum  页码（默认 1）
     * @param pageSize 每页条数（默认 20，最大 50）
     */
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (pageSize > 50) {
            pageSize = 50;
        }
        Long userId = currentUserId();
        log.debug("查询留言列表, userId={}, pageNum={}, pageSize={}", userId, pageNum, pageSize);

        List<MessageBoardResp> list = messageBoardService.listPage(userId, pageNum, pageSize);
        int total = messageBoardService.countAll();

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 发表留言。
     */
    @PostMapping
    public MessageBoardResp create(@RequestBody @Valid MessageBoardReq req) {
        Long userId = currentUserId();
        log.info("发表留言, userId={}", userId);
        return messageBoardService.create(userId, req.getContent());
    }

    /**
     * 删除自己的留言。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long userId = currentUserId();
        messageBoardService.deleteOwn(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 切换点赞/取消点赞。
     */
    @PostMapping("/{id}/like")
    public MessageBoardResp like(@PathVariable Long id) {
        Long userId = currentUserId();
        return messageBoardService.toggleLike(id, userId);
    }

    private Long currentUserId() {
        AuthUser user = UserContext.get();
        if (user == null) {
            throw new BizException(401, "未登录");
        }
        return user.getUserId();
    }
}
