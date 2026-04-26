package com.example.controller;

import com.example.dto.*;
import com.example.entity.*;
import com.example.service.AppService;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AppController {
    @Autowired
    private AppService appService;
    
    @PostMapping("/posts")
    public ResponseEntity<?> createPost(@Valid @RequestBody CreatePostRequest request) {
        try {
            Post post = appService.createPost(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(post);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        try {
            Comment comment = appService.addComment(postId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(comment);
            
        } catch (IllegalStateException e) {
            // Expected guardrail violations - return 429
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "429");
            errorResponse.put("type", "GUARDRAIL_VIOLATION");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            
        } catch (jakarta.persistence.EntityNotFoundException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", "NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            
        } catch (DataAccessException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Database error: " + e.getMostSpecificCause().getMessage());
            errorResponse.put("type", "DATABASE_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("type", e.getClass().getSimpleName());
            errorResponse.put("stack", getStackTrace(e));
            e.printStackTrace(); // Print full stack trace for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> likePost(
            @PathVariable Long postId,
            @Valid @RequestBody LikeRequest request) {
        try {
            appService.likePost(postId, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (int i = 0; i < Math.min(3, stackTrace.length); i++) {
            sb.append(stackTrace[i].toString()).append("; ");
        }
        return sb.toString();
    }
    
}
