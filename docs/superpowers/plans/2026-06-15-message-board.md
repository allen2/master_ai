# 留言板模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增留言板模块：登录用户可发表/查看/点赞/删除自己的留言，管理员可删除任意留言。

**Architecture:** 按现有 Controller → Service → Mapper 三层架构实现，新增 `message_board` 和
`message_board_likes` 两张 SQLite 表；前端新增 `MessageBoardView.vue` 页面和路由，复用
`api/index.js` 的 axios 实例。

**Tech Stack:** Spring Boot 2.7 风格分层 + MyBatis（XML mapper）+ SQLite，Vue 3 + Element Plus + Pinia（前端路由），Vitest（前端测试）/ JUnit5 + Mockito + AssertJ（后端测试）。

---

## Task 1: 数据库表结构

**Files:**
- Modify: `src/main/resources/db/schema.sql`

- [ ] **Step 1: 在文件末尾追加两张表的 DDL**

```sql

-- 留言板主表（所有登录用户可见，逻辑删除）
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

-- 留言点赞记录（唯一索引防止重复点赞，用于判断"我是否已点赞"）
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

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/db/schema.sql
git commit -m "feat: 新增留言板相关数据表"
```

---

## Task 2: DO 模型

**Files:**
- Create: `src/main/java/com/aihedgefund/model/DO/MessageBoardDO.java`
- Create: `src/main/java/com/aihedgefund/model/DO/MessageBoardLikeDO.java`

- [ ] **Step 1: 创建 `MessageBoardDO.java`**

```java
package com.aihedgefund.model.DO;

/**
 * message_board 表映射。
 *
 * <p>{@code nickname} 和 {@code likeRecordId} 为列表关联查询时附加填充的字段，
 * 不对应 message_board 表自身的列。</p>
 */
public class MessageBoardDO {

    private Long id;
    private Long userId;
    private String content;
    private Integer likeCount;
    private Integer deleted;
    private String createdAt;
    private String updatedAt;

    /** 关联查询附加字段：作者昵称（无昵称时回退为用户名） */
    private String nickname;

    /** 关联查询附加字段：当前用户对该留言的点赞记录 id，未点赞为 null */
    private Long likeRecordId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Long getLikeRecordId() { return likeRecordId; }
    public void setLikeRecordId(Long likeRecordId) { this.likeRecordId = likeRecordId; }
}
```

- [ ] **Step 2: 创建 `MessageBoardLikeDO.java`**

```java
package com.aihedgefund.model.DO;

/**
 * message_board_likes 表映射
 */
public class MessageBoardLikeDO {

    private Long id;
    private Long messageId;
    private Long userId;
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aihedgefund/model/DO/MessageBoardDO.java src/main/java/com/aihedgefund/model/DO/MessageBoardLikeDO.java
git commit -m "feat: 新增留言板 DO 模型"
```

---

## Task 3: Req/Resp 模型

**Files:**
- Create: `src/main/java/com/aihedgefund/model/req/MessageBoardReq.java`
- Create: `src/main/java/com/aihedgefund/model/resp/MessageBoardResp.java`

- [ ] **Step 1: 创建 `MessageBoardReq.java`**

```java
package com.aihedgefund.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发表留言入参
 */
public class MessageBoardReq {

    @NotBlank(message = "留言内容不能为空")
    @Size(max = 500, message = "留言内容不能超过 500 字")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
```

- [ ] **Step 2: 创建 `MessageBoardResp.java`**

```java
package com.aihedgefund.model.resp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 留言列表项响应
 */
public class MessageBoardResp {

    private Long id;
    private String nickname;
    private String content;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("like_count")
    private Integer likeCount;

    @JsonProperty("liked_by_me")
    private Boolean likedByMe;

    @JsonProperty("can_delete")
    private Boolean canDelete;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Boolean getLikedByMe() { return likedByMe; }
    public void setLikedByMe(Boolean likedByMe) { this.likedByMe = likedByMe; }
    public Boolean getCanDelete() { return canDelete; }
    public void setCanDelete(Boolean canDelete) { this.canDelete = canDelete; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/aihedgefund/model/req/MessageBoardReq.java src/main/java/com/aihedgefund/model/resp/MessageBoardResp.java
git commit -m "feat: 新增留言板请求/响应模型"
```

---

## Task 4: MessageBoardLikeMapper

**Files:**
- Create: `src/main/java/com/aihedgefund/mapper/MessageBoardLikeMapper.java`
- Create: `src/main/resources/mapper/MessageBoardLikeMapper.xml`
- Test: `src/test/java/com/aihedgefund/mapper/MessageBoardLikeMapperTest.java`

- [ ] **Step 1: 创建 Mapper 接口**

```java
package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.MessageBoardLikeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 留言点赞记录数据访问
 */
@Mapper
public interface MessageBoardLikeMapper {

    /** 查询当前用户对某条留言的点赞记录，未点赞返回 null */
    MessageBoardLikeDO selectByMessageAndUser(@Param("messageId") Long messageId, @Param("userId") Long userId);

    /** 新增点赞记录 */
    int insert(MessageBoardLikeDO like);

    /** 删除点赞记录（取消点赞） */
    int deleteByMessageAndUser(@Param("messageId") Long messageId, @Param("userId") Long userId);
}
```

- [ ] **Step 2: 创建 Mapper XML**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aihedgefund.mapper.MessageBoardLikeMapper">

    <resultMap id="MessageBoardLikeResultMap" type="com.aihedgefund.model.DO.MessageBoardLikeDO">
        <id property="id" column="id"/>
        <result property="messageId" column="message_id"/>
        <result property="userId" column="user_id"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <select id="selectByMessageAndUser" resultMap="MessageBoardLikeResultMap">
        SELECT id, message_id, user_id, created_at, updated_at
        FROM message_board_likes
        WHERE message_id = #{messageId} AND user_id = #{userId}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO message_board_likes (message_id, user_id)
        VALUES (#{messageId}, #{userId})
    </insert>

    <delete id="deleteByMessageAndUser">
        DELETE FROM message_board_likes WHERE message_id = #{messageId} AND user_id = #{userId}
    </delete>

</mapper>
```

- [ ] **Step 3: 编写 Mapper 集成测试**

```java
package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.MessageBoardDO;
import com.aihedgefund.model.DO.MessageBoardLikeDO;
import com.aihedgefund.model.DO.UserDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MessageBoardLikeMapper 集成测试（内存 SQLite）。
 */
@SpringBootTest
class MessageBoardLikeMapperTest {

    @Autowired
    private MessageBoardLikeMapper messageBoardLikeMapper;

    @Autowired
    private MessageBoardMapper messageBoardMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void insertSelectAndDelete_roundTrip() {
        UserDO author = createUser("mbl-mapper-author@example.com");
        UserDO liker = createUser("mbl-mapper-liker@example.com");

        MessageBoardDO message = new MessageBoardDO();
        message.setUserId(author.getId());
        message.setContent("被点赞的留言");
        messageBoardMapper.insert(message);

        assertThat(messageBoardLikeMapper.selectByMessageAndUser(message.getId(), liker.getId())).isNull();

        MessageBoardLikeDO like = new MessageBoardLikeDO();
        like.setMessageId(message.getId());
        like.setUserId(liker.getId());
        messageBoardLikeMapper.insert(like);

        MessageBoardLikeDO found = messageBoardLikeMapper.selectByMessageAndUser(message.getId(), liker.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(like.getId());

        int affected = messageBoardLikeMapper.deleteByMessageAndUser(message.getId(), liker.getId());
        assertThat(affected).isEqualTo(1);
        assertThat(messageBoardLikeMapper.selectByMessageAndUser(message.getId(), liker.getId())).isNull();
    }

    private UserDO createUser(String email) {
        UserDO user = new UserDO();
        user.setUsername(email);
        user.setPasswordHash("test-hash");
        user.setNickname("测试用户-" + email);
        userMapper.insert(user);
        return user;
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvnw test -Dtest=MessageBoardLikeMapperTest`
Expected: BUILD SUCCESS, 1 个测试通过

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aihedgefund/mapper/MessageBoardLikeMapper.java src/main/resources/mapper/MessageBoardLikeMapper.xml src/test/java/com/aihedgefund/mapper/MessageBoardLikeMapperTest.java
git commit -m "feat: 新增留言点赞记录 Mapper"
```

---

## Task 5: MessageBoardMapper

**Files:**
- Create: `src/main/java/com/aihedgefund/mapper/MessageBoardMapper.java`
- Create: `src/main/resources/mapper/MessageBoardMapper.xml`
- Test: `src/test/java/com/aihedgefund/mapper/MessageBoardMapperTest.java`

- [ ] **Step 1: 创建 Mapper 接口**

```java
package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.MessageBoardDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 留言板数据访问
 */
@Mapper
public interface MessageBoardMapper {

    /** 新增留言 */
    int insert(MessageBoardDO message);

    /** 按 id 查询留言（不含 nickname/likeRecordId） */
    MessageBoardDO selectById(@Param("id") Long id);

    /**
     * 分页查询未删除的留言，按 id 倒序，关联查询作者昵称和当前用户的点赞记录。
     *
     * @param currentUserId 当前登录用户 id，用于判断 likedByMe
     * @param offset        偏移量
     * @param limit         每页条数
     */
    List<MessageBoardDO> selectPage(@Param("currentUserId") Long currentUserId,
            @Param("offset") int offset, @Param("limit") int limit);

    /** 统计未删除的留言总数 */
    int countAll();

    /** 逻辑删除留言（仅 deleted=0 时生效），同时更新 updated_at */
    int softDeleteById(@Param("id") Long id);

    /** 原子更新点赞数（delta 为 +1 或 -1），同时更新 updated_at */
    int updateLikeCount(@Param("id") Long id, @Param("delta") int delta);
}
```

- [ ] **Step 2: 创建 Mapper XML**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.aihedgefund.mapper.MessageBoardMapper">

    <resultMap id="MessageBoardResultMap" type="com.aihedgefund.model.DO.MessageBoardDO">
        <id property="id" column="id"/>
        <result property="userId" column="user_id"/>
        <result property="content" column="content"/>
        <result property="likeCount" column="like_count"/>
        <result property="deleted" column="deleted"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
        <result property="nickname" column="nickname"/>
        <result property="likeRecordId" column="like_record_id"/>
    </resultMap>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO message_board (user_id, content)
        VALUES (#{userId}, #{content})
    </insert>

    <select id="selectById" resultMap="MessageBoardResultMap">
        SELECT id, user_id, content, like_count, deleted, created_at, updated_at
        FROM message_board
        WHERE id = #{id}
    </select>

    <select id="selectPage" resultMap="MessageBoardResultMap">
        SELECT mb.id, mb.user_id, mb.content, mb.like_count, mb.deleted, mb.created_at, mb.updated_at,
               COALESCE(u.nickname, u.username) AS nickname,
               mbl.id AS like_record_id
        FROM message_board mb
        JOIN users u ON u.id = mb.user_id
        LEFT JOIN message_board_likes mbl
               ON mbl.message_id = mb.id AND mbl.user_id = #{currentUserId}
        WHERE mb.deleted = 0
        ORDER BY mb.id DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>

    <select id="countAll" resultType="int">
        SELECT COUNT(*) FROM message_board WHERE deleted = 0
    </select>

    <update id="softDeleteById">
        UPDATE message_board
        SET deleted = 1, updated_at = datetime('now')
        WHERE id = #{id} AND deleted = 0
    </update>

    <update id="updateLikeCount">
        UPDATE message_board
        SET like_count = like_count + #{delta}, updated_at = datetime('now')
        WHERE id = #{id}
    </update>

</mapper>
```

- [ ] **Step 3: 编写 Mapper 集成测试**

```java
package com.aihedgefund.mapper;

import com.aihedgefund.model.DO.MessageBoardDO;
import com.aihedgefund.model.DO.UserDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MessageBoardMapper 集成测试（内存 SQLite）。
 */
@SpringBootTest
class MessageBoardMapperTest {

    @Autowired
    private MessageBoardMapper messageBoardMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void insertAndSelectById_returnsMessageWithDefaults() {
        Long userId = createUser("mb-mapper-insert@example.com").getId();

        MessageBoardDO entity = new MessageBoardDO();
        entity.setUserId(userId);
        entity.setContent("第一条留言");
        messageBoardMapper.insert(entity);

        MessageBoardDO saved = messageBoardMapper.selectById(entity.getId());
        assertThat(saved.getContent()).isEqualTo("第一条留言");
        assertThat(saved.getLikeCount()).isEqualTo(0);
        assertThat(saved.getDeleted()).isEqualTo(0);
    }

    @Test
    void selectPage_returnsNewestFirst_withNicknameAndLikedByMe() {
        UserDO author = createUser("mb-mapper-author@example.com");
        UserDO liker = createUser("mb-mapper-liker@example.com");

        MessageBoardDO first = new MessageBoardDO();
        first.setUserId(author.getId());
        first.setContent("留言一");
        messageBoardMapper.insert(first);

        MessageBoardDO second = new MessageBoardDO();
        second.setUserId(author.getId());
        second.setContent("留言二");
        messageBoardMapper.insert(second);

        messageBoardMapper.updateLikeCount(second.getId(), 1);

        List<MessageBoardDO> page = messageBoardMapper.selectPage(liker.getId(), 0, 10);
        assertThat(page.get(0).getId()).isEqualTo(second.getId());
        assertThat(page.get(0).getNickname()).isEqualTo(author.getNickname());
        assertThat(page.get(0).getLikeCount()).isEqualTo(1);
        assertThat(page.get(0).getLikeRecordId()).isNull();
    }

    @Test
    void softDeleteById_excludesFromSelectPageAndCount() {
        Long userId = createUser("mb-mapper-delete@example.com").getId();

        MessageBoardDO entity = new MessageBoardDO();
        entity.setUserId(userId);
        entity.setContent("待删除留言");
        messageBoardMapper.insert(entity);

        int beforeTotal = messageBoardMapper.countAll();

        int affected = messageBoardMapper.softDeleteById(entity.getId());
        assertThat(affected).isEqualTo(1);

        MessageBoardDO deleted = messageBoardMapper.selectById(entity.getId());
        assertThat(deleted.getDeleted()).isEqualTo(1);
        assertThat(messageBoardMapper.countAll()).isEqualTo(beforeTotal - 1);

        List<MessageBoardDO> page = messageBoardMapper.selectPage(userId, 0, 50);
        assertThat(page).extracting(MessageBoardDO::getId).doesNotContain(entity.getId());
    }

    private UserDO createUser(String email) {
        UserDO user = new UserDO();
        user.setUsername(email);
        user.setPasswordHash("test-hash");
        user.setNickname("测试用户-" + email);
        userMapper.insert(user);
        return user;
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvnw test -Dtest=MessageBoardMapperTest`
Expected: BUILD SUCCESS, 3 个测试通过

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aihedgefund/mapper/MessageBoardMapper.java src/main/resources/mapper/MessageBoardMapper.xml src/test/java/com/aihedgefund/mapper/MessageBoardMapperTest.java
git commit -m "feat: 新增留言板 Mapper"
```

---

## Task 6: MessageBoardService

**Files:**
- Create: `src/main/java/com/aihedgefund/service/MessageBoardService.java`
- Test: `src/test/java/com/aihedgefund/service/MessageBoardServiceTest.java`

- [ ] **Step 1: 编写单元测试（先写测试，再写实现）**

```java
package com.aihedgefund.service;

import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.mapper.MessageBoardLikeMapper;
import com.aihedgefund.mapper.MessageBoardMapper;
import com.aihedgefund.mapper.UserMapper;
import com.aihedgefund.model.DO.MessageBoardDO;
import com.aihedgefund.model.DO.MessageBoardLikeDO;
import com.aihedgefund.model.DO.UserDO;
import com.aihedgefund.model.resp.MessageBoardResp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageBoardServiceTest {

    @Mock
    private MessageBoardMapper messageBoardMapper;

    @Mock
    private MessageBoardLikeMapper messageBoardLikeMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MessageBoardService messageBoardService;

    @Test
    void create_returnsRespWithNicknameAndCanDeleteTrue() {
        UserDO user = new UserDO();
        user.setId(1L);
        user.setUsername("user1@example.com");
        user.setNickname("小明");
        when(userMapper.selectById(1L)).thenReturn(user);

        MessageBoardDO saved = new MessageBoardDO();
        saved.setId(10L);
        saved.setUserId(1L);
        saved.setContent("hello");
        saved.setLikeCount(0);
        saved.setDeleted(0);
        saved.setCreatedAt("2026-06-15 10:00:00");
        saved.setUpdatedAt("2026-06-15 10:00:00");
        when(messageBoardMapper.selectById(10L)).thenReturn(saved);

        MessageBoardResp resp = messageBoardService.create(1L, "hello");

        assertThat(resp.getContent()).isEqualTo("hello");
        assertThat(resp.getNickname()).isEqualTo("小明");
        assertThat(resp.getCanDelete()).isTrue();
        assertThat(resp.getLikedByMe()).isFalse();
        verify(messageBoardMapper).insert(any(MessageBoardDO.class));
    }

    @Test
    void deleteOwn_notOwner_throwsForbidden() {
        MessageBoardDO entity = new MessageBoardDO();
        entity.setId(5L);
        entity.setUserId(2L);
        entity.setDeleted(0);
        when(messageBoardMapper.selectById(5L)).thenReturn(entity);

        assertThatThrownBy(() -> messageBoardService.deleteOwn(5L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无权删除");
    }

    @Test
    void deleteOwn_notFound_throws404() {
        when(messageBoardMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> messageBoardService.deleteOwn(99L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void toggleLike_notLikedYet_insertsLikeAndIncreasesCount() {
        MessageBoardDO before = new MessageBoardDO();
        before.setId(7L);
        before.setUserId(2L);
        before.setDeleted(0);
        before.setLikeCount(0);

        MessageBoardDO after = new MessageBoardDO();
        after.setId(7L);
        after.setUserId(2L);
        after.setDeleted(0);
        after.setLikeCount(1);

        when(messageBoardMapper.selectById(7L)).thenReturn(before, after);
        when(messageBoardLikeMapper.selectByMessageAndUser(7L, 1L)).thenReturn(null);
        when(userMapper.selectById(2L)).thenReturn(null);

        MessageBoardResp resp = messageBoardService.toggleLike(7L, 1L);

        verify(messageBoardLikeMapper).insert(any(MessageBoardLikeDO.class));
        verify(messageBoardMapper).updateLikeCount(7L, 1);
        assertThat(resp.getLikedByMe()).isTrue();
        assertThat(resp.getLikeCount()).isEqualTo(1);
    }

    @Test
    void toggleLike_alreadyLiked_removesLikeAndDecreasesCount() {
        MessageBoardDO before = new MessageBoardDO();
        before.setId(7L);
        before.setUserId(2L);
        before.setDeleted(0);
        before.setLikeCount(1);

        MessageBoardDO after = new MessageBoardDO();
        after.setId(7L);
        after.setUserId(2L);
        after.setDeleted(0);
        after.setLikeCount(0);

        when(messageBoardMapper.selectById(7L)).thenReturn(before, after);
        MessageBoardLikeDO existing = new MessageBoardLikeDO();
        existing.setId(100L);
        existing.setMessageId(7L);
        existing.setUserId(1L);
        when(messageBoardLikeMapper.selectByMessageAndUser(7L, 1L)).thenReturn(existing);
        when(userMapper.selectById(2L)).thenReturn(null);

        MessageBoardResp resp = messageBoardService.toggleLike(7L, 1L);

        verify(messageBoardLikeMapper).deleteByMessageAndUser(7L, 1L);
        verify(messageBoardMapper).updateLikeCount(7L, -1);
        assertThat(resp.getLikedByMe()).isFalse();
        assertThat(resp.getLikeCount()).isEqualTo(0);
    }
}
```

- [ ] **Step 2: 运行测试，确认因找不到 `MessageBoardService` 而失败**

Run: `mvnw test -Dtest=MessageBoardServiceTest`
Expected: COMPILE ERROR / FAIL，提示 `MessageBoardService` 不存在

- [ ] **Step 3: 创建 `MessageBoardService.java`**

```java
package com.aihedgefund.service;

import com.aihedgefund.common.exception.BizException;
import com.aihedgefund.mapper.MessageBoardLikeMapper;
import com.aihedgefund.mapper.MessageBoardMapper;
import com.aihedgefund.mapper.UserMapper;
import com.aihedgefund.model.DO.MessageBoardDO;
import com.aihedgefund.model.DO.MessageBoardLikeDO;
import com.aihedgefund.model.DO.UserDO;
import com.aihedgefund.model.resp.MessageBoardResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 留言板业务逻辑：发表、分页查询、点赞切换、删除（本人/管理员）。
 */
@Service
public class MessageBoardService {

    private static final Logger log = LoggerFactory.getLogger(MessageBoardService.class);

    private final MessageBoardMapper messageBoardMapper;
    private final MessageBoardLikeMapper messageBoardLikeMapper;
    private final UserMapper userMapper;

    public MessageBoardService(MessageBoardMapper messageBoardMapper,
            MessageBoardLikeMapper messageBoardLikeMapper, UserMapper userMapper) {
        this.messageBoardMapper = messageBoardMapper;
        this.messageBoardLikeMapper = messageBoardLikeMapper;
        this.userMapper = userMapper;
    }

    /**
     * 发表留言。
     *
     * @param userId  发表人 id
     * @param content 留言内容
     * @return 新建留言详情
     */
    public MessageBoardResp create(Long userId, String content) {
        MessageBoardDO entity = new MessageBoardDO();
        entity.setUserId(userId);
        entity.setContent(content);
        messageBoardMapper.insert(entity);
        log.info("发表留言, id={}, userId={}", entity.getId(), userId);

        MessageBoardDO saved = messageBoardMapper.selectById(entity.getId());
        return toResp(saved, userId, resolveNickname(userId), false);
    }

    /**
     * 分页查询留言列表（按 id 倒序），附带当前用户的点赞状态和删除权限。
     */
    public List<MessageBoardResp> listPage(Long currentUserId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return messageBoardMapper.selectPage(currentUserId, offset, pageSize).stream()
                .map(item -> toResp(item, currentUserId, item.getNickname(), item.getLikeRecordId() != null))
                .collect(Collectors.toList());
    }

    /** 统计未删除的留言总数 */
    public int countAll() {
        return messageBoardMapper.countAll();
    }

    /**
     * 删除自己的留言，非本人留言抛出 403。
     */
    public void deleteOwn(Long id, Long userId) {
        MessageBoardDO entity = getActiveOrThrow(id);
        if (!entity.getUserId().equals(userId)) {
            throw new BizException(403, "无权删除该留言");
        }
        messageBoardMapper.softDeleteById(id);
        log.info("删除留言, id={}, userId={}", id, userId);
    }

    /**
     * 管理员删除任意留言。
     */
    public void deleteByAdmin(Long id) {
        getActiveOrThrow(id);
        messageBoardMapper.softDeleteById(id);
        log.info("管理员删除留言, id={}", id);
    }

    /**
     * 切换点赞/取消点赞状态，返回更新后的留言详情。
     */
    @Transactional
    public MessageBoardResp toggleLike(Long id, Long userId) {
        MessageBoardDO entity = getActiveOrThrow(id);

        MessageBoardLikeDO existing = messageBoardLikeMapper.selectByMessageAndUser(id, userId);
        boolean likedByMe;
        if (existing != null) {
            messageBoardLikeMapper.deleteByMessageAndUser(id, userId);
            messageBoardMapper.updateLikeCount(id, -1);
            likedByMe = false;
            log.info("取消点赞, messageId={}, userId={}", id, userId);
        } else {
            MessageBoardLikeDO like = new MessageBoardLikeDO();
            like.setMessageId(id);
            like.setUserId(userId);
            messageBoardLikeMapper.insert(like);
            messageBoardMapper.updateLikeCount(id, 1);
            likedByMe = true;
            log.info("点赞, messageId={}, userId={}", id, userId);
        }

        MessageBoardDO updated = messageBoardMapper.selectById(id);
        return toResp(updated, userId, resolveNickname(entity.getUserId()), likedByMe);
    }

    /**
     * 查询未逻辑删除的留言，不存在或已删除抛出 404。
     */
    private MessageBoardDO getActiveOrThrow(Long id) {
        MessageBoardDO entity = messageBoardMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new BizException(404, "留言不存在");
        }
        return entity;
    }

    /** 解析昵称，无昵称回退为用户名；用户不存在返回 null */
    private String resolveNickname(Long userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return (user.getNickname() != null && !user.getNickname().isBlank())
                ? user.getNickname() : user.getUsername();
    }

    private MessageBoardResp toResp(MessageBoardDO entity, Long currentUserId, String nickname, boolean likedByMe) {
        MessageBoardResp resp = new MessageBoardResp();
        resp.setId(entity.getId());
        resp.setUserId(entity.getUserId());
        resp.setNickname(nickname);
        resp.setContent(entity.getContent());
        resp.setLikeCount(entity.getLikeCount());
        resp.setLikedByMe(likedByMe);
        resp.setCanDelete(entity.getUserId().equals(currentUserId));
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvnw test -Dtest=MessageBoardServiceTest`
Expected: BUILD SUCCESS, 5 个测试通过

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aihedgefund/service/MessageBoardService.java src/test/java/com/aihedgefund/service/MessageBoardServiceTest.java
git commit -m "feat: 新增留言板业务逻辑"
```

---

## Task 7: MessageBoardController

**Files:**
- Create: `src/main/java/com/aihedgefund/controller/MessageBoardController.java`
- Test: `src/test/java/com/aihedgefund/controller/MessageBoardControllerTest.java`

- [ ] **Step 1: 编写控制器测试**

```java
package com.aihedgefund.controller;

import com.aihedgefund.model.resp.MessageBoardResp;
import com.aihedgefund.service.MessageBoardService;
import com.aihedgefund.support.MockMvcAuthConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MessageBoardController 接口测试：MockMvcAuthConfig 默认以 userId=1 的身份发起请求。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMvcAuthConfig.class)
class MessageBoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageBoardService messageBoardService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void list_returnsPagedMessages() throws Exception {
        MessageBoardResp resp = new MessageBoardResp();
        resp.setId(1L);
        resp.setUserId(1L);
        resp.setNickname("小明");
        resp.setContent("第一条留言");
        resp.setLikeCount(2);
        resp.setLikedByMe(true);
        resp.setCanDelete(true);
        when(messageBoardService.listPage(eq(1L), eq(1), eq(20))).thenReturn(List.of(resp));
        when(messageBoardService.countAll()).thenReturn(1);

        mockMvc.perform(get("/message-board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list[0].content").value("第一条留言"))
                .andExpect(jsonPath("$.list[0].like_count").value(2))
                .andExpect(jsonPath("$.list[0].liked_by_me").value(true))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void create_blankContent_returns422() throws Exception {
        mockMvc.perform(post("/message-board")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void create_validContent_returns200() throws Exception {
        MessageBoardResp resp = new MessageBoardResp();
        resp.setId(1L);
        resp.setUserId(1L);
        resp.setContent("新留言");
        resp.setLikeCount(0);
        resp.setLikedByMe(false);
        resp.setCanDelete(true);
        when(messageBoardService.create(eq(1L), eq("新留言"))).thenReturn(resp);

        mockMvc.perform(post("/message-board")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"新留言\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("新留言"));
    }

    @Test
    void delete_ownMessage_returns204() throws Exception {
        mockMvc.perform(delete("/message-board/1"))
                .andExpect(status().isNoContent());

        verify(messageBoardService).deleteOwn(1L, 1L);
    }

    @Test
    void like_togglesAndReturnsUpdatedCount() throws Exception {
        MessageBoardResp resp = new MessageBoardResp();
        resp.setId(1L);
        resp.setLikeCount(1);
        resp.setLikedByMe(true);
        when(messageBoardService.toggleLike(1L, 1L)).thenReturn(resp);

        mockMvc.perform(post("/message-board/1/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.like_count").value(1))
                .andExpect(jsonPath("$.liked_by_me").value(true));
    }
}
```

- [ ] **Step 2: 运行测试，确认因找不到 `MessageBoardController` 而失败**

Run: `mvnw test -Dtest=MessageBoardControllerTest`
Expected: COMPILE ERROR / FAIL

- [ ] **Step 3: 创建 `MessageBoardController.java`**

```java
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
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvnw test -Dtest=MessageBoardControllerTest`
Expected: BUILD SUCCESS, 5 个测试通过

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aihedgefund/controller/MessageBoardController.java src/test/java/com/aihedgefund/controller/MessageBoardControllerTest.java
git commit -m "feat: 新增留言板接口"
```

---

## Task 8: 管理员删除任意留言

**Files:**
- Modify: `src/main/java/com/aihedgefund/controller/AdminController.java`
- Modify: `src/test/java/com/aihedgefund/controller/AdminControllerTest.java`

- [ ] **Step 1: 在 `AdminControllerTest.java` 中新增测试**

在文件顶部 import 区域新增：

```java
import com.aihedgefund.mapper.MessageBoardMapper;
import com.aihedgefund.model.DO.MessageBoardDO;
```

并将 static import 区域：

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
```

替换为：

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
```

在 `private UserDO createUser(String email) { ... }` 方法之前新增字段（与现有 `userMapper` 字段并列）：

```java
    @Autowired
    private MessageBoardMapper messageBoardMapper;
```

在类内新增两个测试方法（紧邻 `createUser` 方法之前）：

```java
    /**
     * 管理员携带 X-Admin-Token 删除任意留言：留言被标记为已删除。
     */
    @Test
    void deleteMessage_withAdminToken_softDeletes() throws Exception {
        UserDO user = createUser("admin-msg-delete@example.com");
        MessageBoardDO message = new MessageBoardDO();
        message.setUserId(user.getId());
        message.setContent("待管理员删除");
        messageBoardMapper.insert(message);

        mockMvc.perform(delete("/admin/message-board/" + message.getId())
                        .header(ADMIN_TOKEN_HEADER, ADMIN_TOKEN))
                .andExpect(status().isNoContent());

        MessageBoardDO deleted = messageBoardMapper.selectById(message.getId());
        assertThat(deleted.getDeleted()).isEqualTo(1);
    }

    /**
     * 缺少 X-Admin-Token 时应返回 401。
     */
    @Test
    void deleteMessage_missingAdminToken_returns401() throws Exception {
        mockMvc.perform(delete("/admin/message-board/1"))
                .andExpect(status().isUnauthorized());
    }
```

- [ ] **Step 2: 运行测试，确认因 `/admin/message-board/{id}` 不存在而失败（404）**

Run: `mvnw test -Dtest=AdminControllerTest#deleteMessage_withAdminToken_softDeletes`
Expected: FAIL（404 Not Found，断言 isNoContent 失败）

- [ ] **Step 3: 修改 `AdminController.java`**

在 import 区域新增：

```java
import com.aihedgefund.service.MessageBoardService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
```

修改构造方法，注入 `MessageBoardService`：

```java
    private final WalletService walletService;
    private final UserMapper userMapper;
    private final MessageBoardService messageBoardService;

    public AdminController(WalletService walletService, UserMapper userMapper,
            MessageBoardService messageBoardService) {
        this.walletService = walletService;
        this.userMapper = userMapper;
        this.messageBoardService = messageBoardService;
    }
```

在 `listUserBalances` 方法之后新增接口：

```java
    /**
     * 管理员删除任意留言（逻辑删除）。
     */
    @DeleteMapping("/message-board/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        log.info("管理员删除留言, id={}", id);
        messageBoardService.deleteByAdmin(id);
        return ResponseEntity.noContent().build();
    }
```

并在 import 区域新增 `org.springframework.http.ResponseEntity`：

```java
import org.springframework.http.ResponseEntity;
```

- [ ] **Step 4: 运行测试验证通过**

Run: `mvnw test -Dtest=AdminControllerTest`
Expected: BUILD SUCCESS，全部测试通过

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aihedgefund/controller/AdminController.java src/test/java/com/aihedgefund/controller/AdminControllerTest.java
git commit -m "feat: 管理员支持删除任意留言"
```

---

## Task 9: 前端 API 封装与代理配置

**Files:**
- Modify: `frontend/src/api/index.js`
- Modify: `frontend/vite.config.js`

- [ ] **Step 1: 在 `frontend/src/api/index.js` 末尾新增 `messageBoardApi`**

```js
export const messageBoardApi = {
  list:   (pageNum = 1, pageSize = 20) => api.get('/message-board', { params: { pageNum, pageSize } }),
  create: (content)  => api.post('/message-board', { content }),
  remove: (id)        => api.delete(`/message-board/${id}`),
  like:   (id)        => api.post(`/message-board/${id}/like`)
}
```

- [ ] **Step 2: 在 `frontend/vite.config.js` 的 `server.proxy` 中新增 `/message-board` 代理**

```js
      '/message-board':  'http://localhost:8888',
```

（紧跟在 `'/analysis-runs': 'http://localhost:8888',` 之后）

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/index.js frontend/vite.config.js
git commit -m "feat: 新增留言板前端 API 封装"
```

---

## Task 10: 前端留言板页面、路由与导航

**Files:**
- Create: `frontend/src/views/MessageBoardView.vue`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: 创建 `MessageBoardView.vue`**

```vue
<!-- frontend/src/views/MessageBoardView.vue -->
<template>
  <div class="page-view">
    <h2 class="page-title">留言板</h2>

    <div class="post-box">
      <el-input
        v-model="content"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="说点什么吧…"
      />
      <div class="post-actions">
        <el-button type="primary" :loading="posting" :disabled="!content.trim()" @click="handlePost">
          发表
        </el-button>
      </div>
    </div>

    <div v-if="loading && list.length === 0" class="loading-tip">加载中…</div>
    <el-empty v-else-if="list.length === 0" description="暂无留言，来发表第一条吧" />
    <div v-else class="message-list">
      <div v-for="item in list" :key="item.id" class="message-card">
        <div class="message-header">
          <span class="message-author">{{ item.nickname }}</span>
          <span class="message-time">{{ item.created_at }}</span>
        </div>
        <div class="message-content">{{ item.content }}</div>
        <div class="message-actions">
          <button class="like-btn" :class="{ liked: item.liked_by_me }" @click="handleLike(item)">
            👍 {{ item.like_count }}
          </button>
          <el-popconfirm v-if="item.can_delete" title="确认删除该留言？" @confirm="handleDelete(item)">
            <template #reference>
              <button class="delete-btn">删除</button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </div>

    <div v-if="list.length < total" class="load-more">
      <el-button :loading="loading" @click="loadMore">加载更多</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { messageBoardApi } from '../api/index.js'

const list    = ref([])
const total   = ref(0)
const pageNum  = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const posting = ref(false)
const content = ref('')

onMounted(loadPage)

async function loadPage() {
  loading.value = true
  try {
    const data = await messageBoardApi.list(pageNum.value, pageSize.value)
    list.value = pageNum.value === 1 ? data.list : [...list.value, ...data.list]
    total.value = data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  pageNum.value += 1
  await loadPage()
}

async function handlePost() {
  const text = content.value.trim()
  if (!text) {
    return
  }
  posting.value = true
  try {
    const created = await messageBoardApi.create(text)
    list.value.unshift(created)
    total.value += 1
    content.value = ''
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    posting.value = false
  }
}

async function handleLike(item) {
  const wasLiked = item.liked_by_me
  item.liked_by_me = !wasLiked
  item.like_count += wasLiked ? -1 : 1
  try {
    const updated = await messageBoardApi.like(item.id)
    item.liked_by_me = updated.liked_by_me
    item.like_count = updated.like_count
  } catch (e) {
    item.liked_by_me = wasLiked
    item.like_count += wasLiked ? 1 : -1
    ElMessage.error(e.message)
  }
}

async function handleDelete(item) {
  try {
    await messageBoardApi.remove(item.id)
    list.value = list.value.filter((m) => m.id !== item.id)
    total.value -= 1
    ElMessage.success('已删除')
  } catch (e) {
    ElMessage.error(e.message)
  }
}
</script>

<style scoped>
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}
.post-box {
  margin-bottom: 20px;
}
.post-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
.loading-tip {
  font-size: 13px;
  color: #94a3b8;
  padding: 24px 0;
  text-align: center;
}
.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.message-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 16px;
  background: #fff;
}
.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.message-author {
  font-weight: 600;
  color: #0f172a;
  font-size: 13px;
}
.message-time {
  font-size: 12px;
  color: #94a3b8;
}
.message-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.message-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}
.like-btn, .delete-btn {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  padding: 4px 10px;
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
}
.like-btn.liked {
  color: #2563eb;
  border-color: #93c5fd;
  background: #eff6ff;
}
.delete-btn:hover {
  color: #ef4444;
  border-color: #fca5a5;
}
.load-more {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
```

- [ ] **Step 2: 在 `frontend/src/router/index.js` 中新增路由**

在 `import ContrarianAnalysisView from '../views/ContrarianAnalysisView.vue'` 之后新增：

```js
import MessageBoardView from '../views/MessageBoardView.vue'
```

在 `{ path: '/contrarian-analysis', component: ContrarianAnalysisView },` 之后新增：

```js
    { path: '/message-board', component: MessageBoardView },
```

- [ ] **Step 3: 在 `frontend/src/App.vue` 的 `sidebar-nav` 中新增导航入口**

在 `<router-link to="/history"  class="nav-item">📜 分析记录</router-link>` 之后新增：

```html
        <router-link to="/message-board" class="nav-item">💬 留言板</router-link>
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/MessageBoardView.vue frontend/src/router/index.js frontend/src/App.vue
git commit -m "feat: 新增留言板页面、路由与导航入口"
```

---

## Task 11: 前端组件测试

**Files:**
- Create: `frontend/src/views/__tests__/MessageBoardView.test.js`

- [ ] **Step 1: 创建组件测试**

```js
// frontend/src/views/__tests__/MessageBoardView.test.js
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import MessageBoardView from '../MessageBoardView.vue'
import { messageBoardApi } from '../../api/index.js'

vi.mock('../../api/index.js', () => ({
  messageBoardApi: {
    list: vi.fn(),
    create: vi.fn(),
    remove: vi.fn(),
    like: vi.fn()
  }
}))

function makeMessage(overrides = {}) {
  return {
    id: 1,
    user_id: 1,
    nickname: '小明',
    content: '第一条留言',
    like_count: 0,
    liked_by_me: false,
    can_delete: true,
    created_at: '2026-06-15 10:00:00',
    ...overrides
  }
}

describe('MessageBoardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('加载时展示留言列表', async () => {
    messageBoardApi.list.mockResolvedValue({ list: [makeMessage()], total: 1, pageNum: 1, pageSize: 20 })

    const wrapper = mount(MessageBoardView)
    await flushPromises()

    expect(wrapper.text()).toContain('第一条留言')
    expect(wrapper.text()).toContain('小明')
  })

  it('发表留言成功后插入到列表顶部', async () => {
    messageBoardApi.list.mockResolvedValue({ list: [], total: 0, pageNum: 1, pageSize: 20 })
    messageBoardApi.create.mockResolvedValue(makeMessage({ id: 2, content: '新留言' }))

    const wrapper = mount(MessageBoardView)
    await flushPromises()

    wrapper.vm.content = '新留言'
    await wrapper.vm.handlePost()
    await flushPromises()

    expect(messageBoardApi.create).toHaveBeenCalledWith('新留言')
    expect(wrapper.text()).toContain('新留言')
  })

  it('点赞成功后更新点赞数和状态', async () => {
    messageBoardApi.list.mockResolvedValue({ list: [makeMessage()], total: 1, pageNum: 1, pageSize: 20 })
    messageBoardApi.like.mockResolvedValue({ id: 1, like_count: 1, liked_by_me: true })

    const wrapper = mount(MessageBoardView)
    await flushPromises()

    await wrapper.vm.handleLike(wrapper.vm.list[0])
    await flushPromises()

    expect(messageBoardApi.like).toHaveBeenCalledWith(1)
    expect(wrapper.vm.list[0].like_count).toBe(1)
    expect(wrapper.vm.list[0].liked_by_me).toBe(true)
  })

  it('点赞失败时回滚本地状态', async () => {
    messageBoardApi.list.mockResolvedValue({ list: [makeMessage()], total: 1, pageNum: 1, pageSize: 20 })
    messageBoardApi.like.mockRejectedValue(new Error('点赞失败'))

    const wrapper = mount(MessageBoardView)
    await flushPromises()

    await wrapper.vm.handleLike(wrapper.vm.list[0])
    await flushPromises()

    expect(wrapper.vm.list[0].like_count).toBe(0)
    expect(wrapper.vm.list[0].liked_by_me).toBe(false)
  })

  it('删除留言成功后从列表移除', async () => {
    messageBoardApi.list.mockResolvedValue({ list: [makeMessage()], total: 1, pageNum: 1, pageSize: 20 })
    messageBoardApi.remove.mockResolvedValue()

    const wrapper = mount(MessageBoardView)
    await flushPromises()

    await wrapper.vm.handleDelete(wrapper.vm.list[0])
    await flushPromises()

    expect(messageBoardApi.remove).toHaveBeenCalledWith(1)
    expect(wrapper.vm.list).toHaveLength(0)
  })
})

async function flushPromises() {
  await nextTick()
  await new Promise((resolve) => setTimeout(resolve, 0))
  await nextTick()
}
```

- [ ] **Step 2: 运行测试验证通过**

Run: `cd frontend && npm test -- MessageBoardView`
Expected: 全部 5 个测试通过

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/__tests__/MessageBoardView.test.js
git commit -m "test: 新增留言板页面组件测试"
```

---

## Task 12: 更新 README 文档

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 在「页面功能」表格中新增一行**

在 `| `/#/contact` | **联系我们**：联系方式和平台介绍 |` 之后新增：

```
| `/#/message-board` | **留言板**：登录用户可发表留言，所有登录用户可见并可点赞，删除仅限本人留言 |
```

- [ ] **Step 2: 在「后端 API」表格末尾新增四行**

```
| `GET` | `/message-board` | 分页查询留言列表（`pageNum` / `pageSize`） |
| `POST` | `/message-board` | 发表留言 |
| `DELETE` | `/message-board/{id}` | 删除自己的留言 |
| `POST` | `/message-board/{id}/like` | 切换点赞/取消点赞 |
| `DELETE` | `/admin/message-board/{id}` | 管理员删除任意留言 |
```

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: 补充留言板模块说明"
```

---

## Task 13: 整体验证

- [ ] **Step 1: 运行全部后端测试**

Run: `mvnw test`
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行全部前端测试**

Run: `cd frontend && npm test`
Expected: 全部测试通过

- [ ] **Step 3: 前端构建验证**

Run: `cd frontend && npm run build`
Expected: 构建成功，无报错
