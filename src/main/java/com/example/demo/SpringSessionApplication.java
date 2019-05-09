package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.session.data.redis.RedisFlushMode;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
@EnableRedisHttpSession(redisFlushMode = RedisFlushMode.IMMEDIATE)
@Slf4j
public class SpringSessionApplication{

    @RequestMapping("/")
    public String email1(@RegisteredOAuth2AuthorizedClient("salesforce") OAuth2AuthorizedClient authorizedClient) {
        log.info("Hi {}, your token is {}", authorizedClient.getPrincipalName(), authorizedClient.getAccessToken().getTokenValue());
        return "Hi";
    }

    @Bean
    public OAuth2AuthorizedClientRepository repository(){
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    public static ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringSessionApplication.class, args);
    }
}


