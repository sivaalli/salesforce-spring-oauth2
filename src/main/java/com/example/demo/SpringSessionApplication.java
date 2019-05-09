package com.example.demo;

import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
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

    @RequestMapping("/hi")
    public String email(@RegisteredOAuth2AuthorizedClient("salesforce") OAuth2AuthorizedClient authorizedClient, HttpSession session) {
        if (session != null && !session.isNew()){
            log.info("Dyno that received request is {} and session id is {}", System.getenv("DYNO"), session.getId());
        }else {
            log.info("Dyno that received request is {}", System.getenv("DYNO"));
        }
        return "Hello " + authorizedClient.getPrincipalName() + " your access token is " + authorizedClient.getAccessToken().getTokenValue();
    }

    @RequestMapping("/")
    public String email1() {
        return "Hi";
    }

    @Bean
    public static ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringSessionApplication.class, args);
    }
}


