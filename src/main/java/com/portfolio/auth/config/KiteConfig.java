package com.portfolio.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kite")
public class KiteConfig {
    private String apiKey;
    private String apiSecret;
    private String redirectUrl; // must match Zerodha exactly
}