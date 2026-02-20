package com.waiveliability.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.stripe")
@Getter
@Setter
public class StripeConfig {

    private String secretKey;
    private String webhookSecret;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
        }
    }
}
