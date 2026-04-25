package com.example.controller;

import com.example.entity.User;
import com.example.entity.Bot;
import com.example.repo.UserRepository;
import com.example.repo.BotRepository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BotRepository botRepository;

	@Override
	public void run(String... args) throws Exception {
		// test users
		if (userRepository.count() == 0) {
			User user1 = new User();
			user1.setUsername("alice");
			user1.setPremium(false);
			userRepository.save(user1);
			System.out.println(" Created user: alice (ID: " + user1.getId() + ")");

			User user2 = new User();
			user2.setUsername("bob");
			user2.setPremium(true);
			userRepository.save(user2);
			System.out.println(" Created user: bob (ID: " + user2.getId() + ")");
		}

		// test bots
		if (botRepository.count() == 0) {
			List<Bot> bots = new ArrayList<>();

			for (int i = 1; i <= 200; i++) {
				Bot bot = new Bot();
				bot.setName("Bot " + i);
				bot.setPersonaDescription("Auto-generated bot number " + i);
				bots.add(bot);
			}

			botRepository.saveAll(bots);
			System.out.println("✅ Created 200 bots successfully!");
		}

	}
}