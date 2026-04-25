package com.example.dto;

import jakarta.validation.constraints.NotNull;

public class LikeRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public LikeRequest(@NotNull(message = "User ID is required") Long userId) {
		super();
		this.userId = userId;
	}
    
}