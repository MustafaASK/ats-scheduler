package com.ask.ats.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * The type Rest template config.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${bullhorn.connectionTimeout:10}")
    private int connectionTimeOut;

    @Value("${bullhorn.readTimeout:60}")
    private int readTimeOut;

    /**
     * Rest template rest template.
     *
     * @param builder the builder
     * @return the rest template
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(connectionTimeOut))
                .setReadTimeout(Duration.ofSeconds(readTimeOut)).build();
    }
}
