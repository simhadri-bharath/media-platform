package com.bharath.media_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCaching
public class MediaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediaBackendApplication.class, args);
	}

}
