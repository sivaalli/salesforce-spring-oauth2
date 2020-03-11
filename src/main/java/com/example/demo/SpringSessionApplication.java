package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

@Controller
@SpringBootApplication
//@EnableRedisHttpSession(redisFlushMode = RedisFlushMode.IMMEDIATE)
@Slf4j
public class SpringSessionApplication{

    private static final String USER_INFO_URI = "https://%s.auth-qa1.marketingcloudqaapis.com/v2/userinfo";
    private static final String REST_URL = "https://(\\S+).rest.marketingcloudapis.com";


    @GetMapping("/")
    public String email1(Model model, @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
                         @AuthenticationPrincipal OAuth2User oAuth2User) {
        model.addAttribute("userName", oAuth2User.getName());
        model.addAttribute("clientName", authorizedClient.getClientRegistration().getClientName());
        model.addAttribute("userAttributes", oAuth2User.getAttributes());
        return "index";
    }

    @Bean
    public OAuth2AuthorizedClientRepository repository(){
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> userService() {
        return new OAuth2UserService<OAuth2UserRequest, OAuth2User>() {

            private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
                final String restUrl = userRequest.getAdditionalParameters().get("rest_instance_url").toString();
                final String tssd = restUrl.substring(8, 8 + 28); // TSSD is 28 character length.
                final ClientRegistration registration = ClientRegistration.withClientRegistration(userRequest.getClientRegistration())
                                                                          .userInfoUri(String.format(USER_INFO_URI, tssd))
                                                                          .build();
                final OAuth2UserRequest request = new OAuth2UserRequest(registration, userRequest.getAccessToken(), userRequest.getAdditionalParameters());
                return delegate.loadUser(request);
            }
        };
    }

//    @Bean
//    public static ConfigureRedisAction configureRedisAction() {
//        return ConfigureRedisAction.NO_OP;
//    }

    public static void main(String[] args) {
        SpringApplication.run(SpringSessionApplication.class, args);
    }
}


