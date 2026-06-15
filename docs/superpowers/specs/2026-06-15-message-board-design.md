# 留言板模块设计

## 背景与目标

新增一个留言板模块：登录用户可以发表文字留言，所有登录用户都可以查看留言列表并点赞；
留言作者可以删除自己的留言，管理员可以删除任意留言。

## 数据模型

新增两张表，写入 `src/main/resources/db/schema.sql`（SQLite 语法，风格与现有表一致）。

### message_board（留言主表）

```sql
CREATE TABLE IF NOT EXISTS message_board (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    content     TEXT    NOT NULL,
    like_count  INTEGER NOT NULL DEFAULT 0,
    deleted     INTEGER NOT NULL DEFAULT 0,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_message_board_created_at ON message_board(created_at);
CREATE INDEX IF NOT EXISTS idx_message_board_user_id ON message_board(user_id);
```

- `content`：最大长度 500 字符（接口层用 `@Size` 校验）
- `like_count`：缓存点赞数，避免每次查询都聚合统计
- `deleted`：逻辑删除标记（0/1），删除时同时更新 `updated_at`

### message_board_likes（点赞记录表）

```sql
CREATE TABLE IF NOT EXISTS message_board_likes (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id  INTEGER NOT NULL,
    user_id     INTEGER NOT NULL,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_message_board_likes_msg_user
    ON message_board_likes(message_id, user_id);
```

- 唯一索引 `(message_id, user_id)` 防止重复点赞，同时用于判断"我是否已点赞"

## 后端设计

遵循现有分层架构（Controller → Service → Mapper），统一返回格式，
复用 `BizException` + `GlobalExceptionHandler` 处理异常。

### 新建文件

- `model/DO/MessageBoardDO.java` — message_board 表映射
- `model/DO/MessageBoardLikeDO.java` — message_board_likes 表映射
- `model/req/MessageBoardReq.java` — 发表留言请求体（`content`，`@NotBlank @Size(max=500)`）
- `model/resp/MessageBoardResp.java` — 留言列表项响应（`id`, `userId`, `nickname`, `content`,
  `likeCount`, `likedByMe`, `canDelete`, `createTime`, `updateTime`）
- `mapper/MessageBoardMapper.java` + 对应 XML
- `mapper/MessageBoardLikeMapper.java` + 对应 XML
- `service/MessageBoardService.java`
- `controller/MessageBoardController.java`

### API 接口

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/message-board?pageNum=1&pageSize=20` | 分页查询留言列表，按 `created_at` 倒序 | JWT（登录用户） |
| POST | `/message-board` | 发表留言，请求体 `{content}` | JWT |
| DELETE | `/message-board/{id}` | 删除自己的留言；非本人留言返回 403（`BizException`） | JWT |
| POST | `/message-board/{id}/like` | 切换点赞/取消点赞，返回最新 `likeCount` 与 `likedByMe` | JWT |
| DELETE | `/admin/message-board/{id}` | 管理员删除任意留言 | `X-Admin-Token`（复用 `AdminInterceptor`） |

### 业务逻辑要点

1. **列表查询**：`MessageBoardMapper` 通过 SQL `JOIN users` 取作者昵称
   （`nickname` 为空时回退为 `username`），并 `LEFT JOIN message_board_likes`
   按当前用户 id 判断 `likedByMe`。`canDelete` 在 Service 层根据
   `entity.userId == UserContext.get().getUserId()` 计算。
2. **点赞切换**：在 Service 层用事务包裹——
   - 若 `message_board_likes` 中已存在该 `(message_id, user_id)` 记录则删除，并将
     `message_board.like_count` 减 1；
   - 否则插入记录并将 `like_count` 加 1。
   - `like_count` 更新使用 `UPDATE ... SET like_count = like_count + 1` 原子语句，避免先查后写的竞态。
3. **删除留言**：逻辑删除（`deleted = 1`），同时更新 `updated_at`。本人删除走 `/message-board/{id}`；
   管理员删除走 `/admin/message-board/{id}`，二者复用 `MessageBoardService.deleteById`，
   仅权限校验位置不同。
4. **日志**：发表/删除/点赞均按规范输出 info/debug 日志（用户 id、留言 id、操作结果）。

## 前端设计

### 新页面

- `frontend/src/views/MessageBoardView.vue`
- 路由：`router/index.js` 中新增 `{ path: '/message-board', component: MessageBoardView }`
  （非公开路径，走现有登录守卫逻辑）
- 导航入口：`App.vue` 的 `sidebar-nav` 中新增一项，例如
  `<router-link to="/message-board" class="nav-item">💬 留言板</router-link>`

### API 封装

在现有 `frontend/src/api/index.js` 中新增（不创建新文件，与现有模块风格一致）：

```js
export const messageBoardApi = {
  list:   (pageNum = 1, pageSize = 20) => api.get('/message-board', { params: { pageNum, pageSize } }),
  create: (content)  => api.post('/message-board', { content }),
  remove: (id)        => api.delete(`/message-board/${id}`),
  like:   (id)        => api.post(`/message-board/${id}/like`)
}
```

### 页面结构与交互

- 顶部：留言输入框（`textarea`，0/500 字数提示）+ "发表"按钮
- 列表：每条留言展示昵称、内容、创建时间、点赞按钮（带数量，`likedByMe` 时高亮）、
  删除按钮（仅 `canDelete` 为 true 时显示）
- 底部："加载更多"按钮，点击后追加下一页数据（基于 `pageNum`）
- 发表成功后将新留言插入列表顶部
- 点赞按钮采用乐观更新（先切换本地状态，再调用接口；接口失败时回滚）
- 删除前弹出二次确认

## 测试

- 后端：为 `MessageBoardService`（点赞切换的事务逻辑、权限校验）和
  `MessageBoardController`（接口级 FT）编写单元测试/集成测试
- 前端：为 `MessageBoardView.vue` 的发表/点赞/删除/分页加载补充测试
- 同步更新 API 文档与 README

## 范围说明（不在本次实现范围内）

- 不支持留言回复（楼层/嵌套展示）
- 不支持富文本/图片留言，仅纯文本
- 不支持留言编辑，仅支持删除后重新发表
