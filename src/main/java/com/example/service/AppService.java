package com.example.service;

import com.example.dto.*;
import com.example.entity.*;
import com.example.redis.RedisViralityService;
import com.example.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;

@Service
public class AppService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BotRepository botRepository;
    
    @Autowired
    private RedisViralityService redisViralityService;
    
    @Transactional
    public Post createPost(CreatePostRequest request) {
        if ("USER".equalsIgnoreCase(request.getAuthorType())) {
            userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getAuthorId()));
        } else if ("BOT".equalsIgnoreCase(request.getAuthorType())) {
            botRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new EntityNotFoundException("Bot not found with id: " + request.getAuthorId()));
        } else {
            throw new IllegalArgumentException("Author type must be USER or BOT");
        }
        
        Post post = new Post();
        post.setAuthorId(request.getAuthorId());
        post.setAuthorType(request.getAuthorType().toUpperCase());
        post.setContent(request.getContent());
        
        Post savedPost = postRepository.save(post);
        System.out.println(" Post created: ID " + savedPost.getId() + " by " + request.getAuthorType() + " " + request.getAuthorId());
        
        return savedPost;
    }
    
    @Transactional
    public Comment addComment(Long postId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));
        
        String authorName = null;
        if ("USER".equalsIgnoreCase(request.getAuthorType())) {
            User user = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getAuthorId()));
            authorName = user.getUsername();
        } else if ("BOT".equalsIgnoreCase(request.getAuthorType())) {
            Bot bot = botRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new EntityNotFoundException("Bot not found with id: " + request.getAuthorId()));
            authorName = bot.getName();
        } else {
            throw new IllegalArgumentException("Author type must be USER or BOT");
        }
        
        int depthLevel = 0;
        if (request.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                .orElseThrow(() -> new EntityNotFoundException("Parent comment not found"));
            depthLevel = parentComment.getDepthLevel() + 1;
        }
        
        if (!redisViralityService.isValidCommentDepth(depthLevel)) {
            throw new IllegalStateException("Comment thread cannot exceed 20 levels deep");
        }
        
        if ("BOT".equalsIgnoreCase(request.getAuthorType())) {
            if (!redisViralityService.canBotReplyToPost(postId)) {
                throw new IllegalStateException("Post has reached maximum of 100 bot replies");
            }
            
            Long humanId = post.getAuthorId();
            Long botId = request.getAuthorId();
            
            if (!redisViralityService.canBotInteractWithHuman(botId, humanId)) {
                throw new IllegalStateException("Bot cannot interact with this user more than once per 10 minutes");
            }
        }
        
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(request.getAuthorId());
        comment.setAuthorType(request.getAuthorType().toUpperCase());
        comment.setContent(request.getContent());
        comment.setParentCommentId(request.getParentCommentId());
        comment.setDepthLevel(depthLevel);
        
        Comment savedComment = commentRepository.save(comment);
        System.out.println(" Comment added: ID " + savedComment.getId() + " on post " + postId);
        
        if ("BOT".equalsIgnoreCase(request.getAuthorType())) {
            redisViralityService.incrementViralityScore(postId, "bot_reply");
            
            User postAuthor = userRepository.findById(post.getAuthorId()).orElse(null);
            if (postAuthor != null) {
                String notificationMsg = "Bot " + authorName + " replied to your post: \"" + 
                    truncateContent(post.getContent(), 50) + "\"";
                
                redisViralityService.shouldSendNotificationNow(post.getAuthorId(), notificationMsg);
            }
            
        } else if ("USER".equalsIgnoreCase(request.getAuthorType())) {
            redisViralityService.incrementViralityScore(postId, "human_comment");
        }
        
        return savedComment;
    }
    
    @Transactional
    public void likePost(Long postId, LikeRequest request) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new EntityNotFoundException("Post not found with id: " + postId));
        
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getUserId()));
        
        redisViralityService.incrementViralityScore(postId, "human_like");
        
        System.out.println(" User " + user.getUsername() + " liked post " + postId);
    }
    
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
    
    public void printPostStats(Long postId) {
        System.out.println("\n STATS for Post " + postId + ":");
        System.out.println("  - Bot replies: " + redisViralityService.getBotCountForPost(postId));
        System.out.println("  - Virality score: " + redisViralityService.getViralityScore(postId));
    }
}