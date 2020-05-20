package com.example.demo;

import com.example.demo.EtClient.Item;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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
import org.springframework.session.FlushMode;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@SpringBootApplication
@EnableRedisHttpSession(flushMode = FlushMode.IMMEDIATE)
@Slf4j
public class SpringSessionApplication {
    private static final Logger logger = LoggerFactory.getLogger(SpringSessionApplication.class);

    private static final String USER_INFO_URI = "https://%s.auth-qa1.marketingcloudqaapis.com/v2/userinfo";
    private static final String ET_REST_TSSD =
            "https://mc882zkqmv0jpg-dz8-ng69z7ws4.rest-qa1.marketingcloudqaapis.com/";

    @Autowired
    private EtClient etClient;

    @Bean
    public static ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringSessionApplication.class, args);
    }

    @GetMapping("/")
    public String email1(
            Model model, @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        final List<Item> permissions = etClient.permissions(authorizedClient.getAccessToken().getTokenValue());
        model.addAttribute("userName", oAuth2User.getName());

        final Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("token", authorizedClient.getAccessToken().getTokenValue());
        attributes.put("permissions", permissions);
        model.addAttribute("clientName", authorizedClient.getClientRegistration().getClientName());
        model.addAttribute("userAttributes", attributes);
        return "index";
    }

    @Bean
    public OAuth2AuthorizedClientRepository repository() {
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
                final ClientRegistration registration =
                        ClientRegistration.withClientRegistration(userRequest.getClientRegistration())
                                .userInfoUri(String.format(USER_INFO_URI, tssd))
                                .build();
                logger.info("UserInfo route is {}", registration.getProviderDetails().getUserInfoEndpoint().getUri());
                final OAuth2UserRequest request = new OAuth2UserRequest(registration,
                                                                        userRequest.getAccessToken(),
                                                                        userRequest.getAdditionalParameters());
                return delegate.loadUser(request);
            }
        };
    }


    @Bean
    public RestTemplate template(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(5))
                .rootUri(ET_REST_TSSD)
                // use apache http client
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .build();
    }
}


