package com.waiveliability.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

import java.net.URI;
import java.util.Properties;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class EmailConfig {

    private String region;
    private String emailFrom;
    private AwsProperties aws = new AwsProperties();

    @Getter
    @Setter
    public static class AwsProperties {
        private SesProperties ses = new SesProperties();
    }

    @Getter
    @Setter
    public static class SesProperties {
        private String accessKey;
        private String secretKey;
    }

    @Bean
    public SesClient sesClient() {
        var builder = SesClient.builder()
            .region(Region.of(region));

        if (aws.getSes() != null &&
            aws.getSes().getAccessKey() != null &&
            !aws.getSes().getAccessKey().isEmpty() &&
            aws.getSes().getSecretKey() != null &&
            !aws.getSes().getSecretKey().isEmpty()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(aws.getSes().getAccessKey(), aws.getSes().getSecretKey())
            ));
        }

        return builder.build();
    }

    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }
}
