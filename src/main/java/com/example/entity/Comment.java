package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id", nullable = false)
    private Long postId;
    
    @Column(name = "author_id", nullable = false)
    private Long authorId;
    
    @Column(name = "author_type", nullable = false)
    private String authorType; // "USER" or "BOT"
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "parent_comment_id")
    private Long parentCommentId;
    
    @Column(name = "depth_level", nullable = false)
    private Integer depthLevel = 0;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

	public Long getParentCommentId() {
		return parentCommentId;
	}

	public void setParentCommentId(Long parentCommentId) {
		this.parentCommentId = parentCommentId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPostId() {
		return postId;
	}

	public void setPostId(Long postId) {
		this.postId = postId;
	}

	public Long getAuthorId() {
		return authorId;
	}

	public void setAuthorId(Long authorId) {
		this.authorId = authorId;
	}

	public String getAuthorType() {
		return authorType;
	}

	public void setAuthorType(String authorType) {
		this.authorType = authorType;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getDepthLevel() {
		return depthLevel;
	}

	public void setDepthLevel(Integer depthLevel) {
		this.depthLevel = depthLevel;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Comment() {
		super();
	}

	public Comment(Long id, Long postId, Long authorId, String authorType, String content, Long parentCommentId,
			Integer depthLevel, LocalDateTime createdAt) {
		super();
		this.id = id;
		this.postId = postId;
		this.authorId = authorId;
		this.authorType = authorType;
		this.content = content;
		this.parentCommentId = parentCommentId;
		this.depthLevel = depthLevel;
		this.createdAt = createdAt;
	}
    
}