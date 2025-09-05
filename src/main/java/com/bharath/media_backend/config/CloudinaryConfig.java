package com.bharath.media_backend.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Value;

@Configuration
public class CloudinaryConfig {

    @Value("${CLOUDINARY_CLOUD_NAME}")
    private String cloudName;

    @Value("${CLOUDINARY_API_KEY}")
    private String apiKey;

    @Value("${CLOUDINARY_API_SECRET}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        String cloudinaryUrl = String.format(
            "cloudinary://%s:%s@%s", apiKey, apiSecret, cloudName
        );
        return new Cloudinary(cloudinaryUrl);
    }
}


