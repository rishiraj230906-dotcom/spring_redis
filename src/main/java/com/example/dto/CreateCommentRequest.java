package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCommentRequest {
    
    @NotNull(message = "Author ID is required")
    private Long authorId;
    
    @NotBlank(message = "Author type is required (USER or BOT)")
    private String authorType;
    
    @NotBlank(message = "Content cannot be empty")
    private String content;
    
    private Long parentCommentId;

	public Long getParentCommentId() {
		return parentCommentId;
	}

	public void setParentCommentId(Long parentCommentId) {
		this.parentCommentId = parentCommentId;
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

	public CreateCommentRequest(@NotNull(message = "Author ID is required") Long authorId,
			@NotBlank(message = "Author type is required (USER or BOT)") String authorType,
			@NotBlank(message = "Content cannot be empty") String content, Long parentCommentId) {
		super();
		this.authorId = authorId;
		this.authorType = authorType;
		this.content = content;
	}
    
}