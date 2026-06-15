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
