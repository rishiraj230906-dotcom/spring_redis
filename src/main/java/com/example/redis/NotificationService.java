package com.example.redis;

import com.example.entity.User;
import com.example.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@EnableScheduling
public class NotificationService {
    
    @Autowired
    private RedisViralityService redisViralityService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Scheduled(cron = "0 */5 * * * *") 
    public void sweepPendingNotifications() {
        System.out.println("");
        
        Set<String> pendingKeys = redisViralityService.getAllUsersWithPendingNotifications();
        
        if (pendingKeys.isEmpty()) {
            System.out.println(" No pending notifications found in Redis.");
            return;
        }
        
        System.out.println(" Found " + pendingKeys.size() + " user(s) with pending notifications\n");
        
        for (String key : pendingKeys) {
            Long userId = redisViralityService.extractUserIdFromKey(key);
            if (userId == null) {
                continue;
            }
            
            User user = userRepository.findById(userId).orElse(null);
            String username = (user != null) ? user.getUsername() : "Unknown User";
            
            List<Object> pendingNotifs = redisViralityService.getAndClearPendingNotifications(userId);
            
            if (pendingNotifs != null && !pendingNotifs.isEmpty()) {
                String summary = summarizeNotifications(pendingNotifs);
                
                System.out.println(" [SUMMARIZED PUSH NOTIFICATION]");
                System.out.println("   User: " + username + " (ID: " + userId + ")");
                System.out.println("   Summary: " + summary);
                System.out.println("   Original count: " + pendingNotifs.size() + " notification(s) batched into 1");
                
            }
        }
        
        System.out.println("\n Sweep completed at " + new java.util.Date());
    }
    
    private String summarizeNotifications(List<Object> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return "No interactions";
        }
        
        if (notifications.size() == 1) {
            return extractBotName((String) notifications.get(0));
        }
        
        String firstBot = extractBotName((String) notifications.get(0));
        int otherCount = notifications.size() - 1;
        
        if (otherCount == 1) {
            return firstBot + " and 1 other interacted with your posts.";
        } else {
            return firstBot + " and " + otherCount + " others interacted with your posts.";
        }
    }
    
    private String extractBotName(String notification) {
        if (notification == null) {
            return "A bot";
        }
        
        Pattern pattern = Pattern.compile("^(Bot [^ ]+)");
        Matcher matcher = pattern.matcher(notification);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return "A bot";
    }
    
    public void manualSweep() {
        System.out.println(" Manual notification sweep triggered");
        sweepPendingNotifications();
    }
}