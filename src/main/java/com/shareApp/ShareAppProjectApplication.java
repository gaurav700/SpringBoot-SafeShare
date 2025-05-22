package com.shareApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.shareApp.repositories")
public class ShareAppProjectApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShareAppProjectApplication.class, args);
	}
}

