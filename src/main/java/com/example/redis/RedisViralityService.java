package com.example.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class RedisViralityService {
    
    private static final int MAX_BOT_REPLIES = 100;
    private static final int MAX_COMMENT_DEPTH = 20;
    private static final long BOT_HUMAN_COOLDOWN_MINUTES = 10;
    private static final long NOTIFICATION_COOLDOWN_MINUTES = 15;
    private static final String NOTIFICATION_COOLDOWN_PREFIX = "user:";
    private static final String NOTIFICATION_COOLDOWN_SUFFIX = ":last_notification_time";
    private static final String PENDING_NOTIFS_PREFIX = "user:";
    private static final String PENDING_NOTIFS_SUFFIX = ":pending_notifs";
    private static final int BOT_REPLY_POINTS = 1;
    private static final int HUMAN_LIKE_POINTS = 20;
    private static final int HUMAN_COMMENT_POINTS = 50;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void incrementViralityScore(Long postId, String interactionType) {
        String key = "post:" + postId + ":virality_score";
        int points = getPointsForInteraction(interactionType);
        
        if (points > 0) {
            Long newScore = redisTemplate.opsForValue().increment(key, points);
            System.out.println(" [VIRALITY] Post " + postId + " +" + points + 
                " points (New score: " + newScore + ") from " + interactionType);
        }
    }
    
    public Long getViralityScore(Long postId) {
        String key = "post:" + postId + ":virality_score";
        Object score = redisTemplate.opsForValue().get(key);
        return score == null ? 0L : ((Number) score).longValue();
    }
    
    private int getPointsForInteraction(String type) {
        switch (type.toLowerCase()) {
            case "bot_reply": return BOT_REPLY_POINTS;
            case "human_like": return HUMAN_LIKE_POINTS;
            case "human_comment": return HUMAN_COMMENT_POINTS;
            default: return 0;
        }
    }

    public boolean canBotReplyToPost(Long postId) {
        String key = "post:" + postId + ":bot_count";
        
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        System.out.println(" [HORIZONTAL CAP] Post " + postId + 
            " bot count: " + currentCount + "/" + MAX_BOT_REPLIES);
        
        if (currentCount > MAX_BOT_REPLIES) {
            redisTemplate.opsForValue().decrement(key);
            System.out.println(" [HORIZONTAL CAP] REJECTED! Post " + postId + 
                " has reached max bot replies");
            return false;
        }
        
        System.out.println(" [HORIZONTAL CAP] ALLOWED - Bot can reply to post " + postId);
        return true;
    }
    
    public Long getBotCountForPost(Long postId) {
        String key = "post:" + postId + ":bot_count";
        Object count = redisTemplate.opsForValue().get(key);
        return count == null ? 0L : ((Number) count).longValue();
    }
    
    public boolean isValidCommentDepth(int currentDepth) {
        boolean isValid = currentDepth <= MAX_COMMENT_DEPTH;
        
        if (!isValid) {
            System.out.println(" [VERTICAL CAP] REJECTED! Depth " + currentDepth + 
                " exceeds maximum of " + MAX_COMMENT_DEPTH);
        } else {
            System.out.println(" [VERTICAL CAP] Depth " + currentDepth + " is valid");
        }
        
        return isValid;
    }
    
    public boolean canBotInteractWithHuman(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        
        Boolean setSuccess = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofMinutes(BOT_HUMAN_COOLDOWN_MINUTES));
        
        if (Boolean.TRUE.equals(setSuccess)) {
            System.out.println(" [COOLDOWN CAP] Bot " + botId + 
                " can interact with Human " + humanId + 
                " (Cooldown set for " + BOT_HUMAN_COOLDOWN_MINUTES + " minutes)");
            return true;
        } else {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            System.out.println(" [COOLDOWN CAP] REJECTED! Bot " + botId + 
                " cannot interact with Human " + humanId + 
                " (Cooldown active for " + ttl + " more seconds)");
            return false;
        }
    }
    
    public void clearCooldown(Long botId, Long humanId) {
        String key = "cooldown:bot_" + botId + ":human_" + humanId;
        redisTemplate.delete(key);
        System.out.println(" [COOLDOWN] Cleared cooldown for Bot " + botId + 
            " -> Human " + humanId);
    }
    
    public void resetPostData(Long postId) {
        String botCountKey = "post:" + postId + ":bot_count";
        String viralityKey = "post:" + postId + ":virality_score";
        
        redisTemplate.delete(botCountKey);
        redisTemplate.delete(viralityKey);
        
        System.out.println("[RESET] Reset all Redis data for post " + postId);
    }
    
    public void printPostState(Long postId) {
        System.out.println("\n [STATE] Post " + postId + ":");
        System.out.println("   Bot Count: " + getBotCountForPost(postId) + "/" + MAX_BOT_REPLIES);
        System.out.println("   Virality Score: " + getViralityScore(postId));
        System.out.println("   Virality Breakdown: Bot Reply=+1, Human Like=+20, Human Comment=+50\n");
    }
    
    public boolean shouldSendNotificationNow(Long userId, String notificationMessage) {
        String cooldownKey = NOTIFICATION_COOLDOWN_PREFIX + userId + NOTIFICATION_COOLDOWN_SUFFIX;
        String pendingKey = PENDING_NOTIFS_PREFIX + userId + PENDING_NOTIFS_SUFFIX;
        
        Boolean cooldownExists = redisTemplate.hasKey(cooldownKey);
        
        if (Boolean.TRUE.equals(cooldownExists)) {
            redisTemplate.opsForList().rightPush(pendingKey, notificationMessage);
            Long pendingCount = redisTemplate.opsForList().size(pendingKey);
            System.out.println(" [THROTTLED] User " + userId + " - notification queued (pending: " + pendingCount + ")");
            return false;
        } else {
            System.out.println(" [IMMEDIATE] Push Notification Sent to User " + userId + ": " + notificationMessage);
            redisTemplate.opsForValue().set(cooldownKey, System.currentTimeMillis(), 
                Duration.ofMinutes(NOTIFICATION_COOLDOWN_MINUTES));
            return true;
        }
    }
    public List<Object> getPendingNotifications(Long userId) {
        String pendingKey = PENDING_NOTIFS_PREFIX + userId + PENDING_NOTIFS_SUFFIX;
        List<Object> pending = redisTemplate.opsForList().range(pendingKey, 0, -1);
        return pending != null ? pending : new ArrayList<>();
    }
    
    public List<Object> getAndClearPendingNotifications(Long userId) {
        String pendingKey = PENDING_NOTIFS_PREFIX + userId + PENDING_NOTIFS_SUFFIX;
        
        List<Object> pending = redisTemplate.opsForList().range(pendingKey, 0, -1);
        
        if (pending != null && !pending.isEmpty()) {
            redisTemplate.delete(pendingKey);
            System.out.println(" Cleared " + pending.size() + " pending notifications for user " + userId);
        }
        
        return pending != null ? pending : new ArrayList<>();
    }
    
    public boolean hasPendingNotifications(Long userId) {
        String pendingKey = PENDING_NOTIFS_PREFIX + userId + PENDING_NOTIFS_SUFFIX;
        Long size = redisTemplate.opsForList().size(pendingKey);
        return size != null && size > 0;
    }
    
    public Long getPendingNotificationCount(Long userId) {
        String pendingKey = PENDING_NOTIFS_PREFIX + userId + PENDING_NOTIFS_SUFFIX;
        Long size = redisTemplate.opsForList().size(pendingKey);
        return size != null ? size : 0L;
    }
    
    public Set<String> getAllUsersWithPendingNotifications() {
        String pattern = PENDING_NOTIFS_PREFIX + "*" + PENDING_NOTIFS_SUFFIX;
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null ? keys : java.util.Collections.emptySet();
    }
    
    public Long extractUserIdFromKey(String key) {
        try {
            String[] parts = key.split(":");
            if (parts.length >= 2) {
                return Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException e) {
            System.err.println("Failed to load user ID from key: " + key);
        }
        return null;
    }
}