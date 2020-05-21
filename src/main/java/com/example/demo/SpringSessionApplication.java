package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
@SpringBootApplication
@EnableRedisHttpSession(flushMode = FlushMode.IMMEDIATE)
@Slf4j
public class SpringSessionApplication {
    private static final Logger logger = LoggerFactory.getLogger(SpringSessionApplication.class);

    private static final String USER_INFO_URI = "https://%s.auth-qa1.marketingcloudqaapis.com/v2/userinfo";
    private static final String INTERACTION_STUDIO_APP = "InteractionStudioV2";
    private static final String PERMISSION_FILTER_OBJECT_TYPE = "ObjectType";
    private static final String PERMISSIONS_INFO_URI = "/platform-internal/v1/users/@current/permissions?$pageSize=1000&$filter=%s";

    @Bean
    public static ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringSessionApplication.class, args);
    }

    @GetMapping("/")
    public String indexPage(
            Model model,
            @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient,
            @AuthenticationPrincipal OAuth2User oAuth2User) {
        model.addAttribute("method", "UserInfo & Permissions");

        model.addAttribute("userName", oAuth2User.getName());
        model.addAttribute("clientName", authorizedClient.getClientRegistration().getClientName());

        final Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("token", authorizedClient.getAccessToken().getTokenValue());
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

            private final McOAuth2UserService delegate = new McOAuth2UserService();

            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
                final String restUrl = userRequest.getAdditionalParameters().get("rest_instance_url").toString();
                final String tssd = restUrl.substring(8, 8 + 28); // TSSD is 28 character length.

                final OAuth2UserRequest oauth2UserRequest = createRequest(userRequest,
                                                                          String.format(USER_INFO_URI, tssd),
                                                                          "User");
                final OAuth2UserRequest oauth2PermissionRequest = createRequest(userRequest,
                                                                                getPermissionRoute(restUrl),
                                                                                "Permission");

                return delegate.loadUser(oauth2UserRequest, oauth2PermissionRequest);
            }
        };
    }

    private OAuth2UserRequest createRequest(final OAuth2UserRequest userRequest, final String targetUri, final String requestType) {
        final ClientRegistration registration =
                ClientRegistration.withClientRegistration(userRequest.getClientRegistration())
                        .userInfoUri(targetUri)
                        .build();
        logger.info("{} UserInfo route is {}", requestType, registration.getProviderDetails().getUserInfoEndpoint().getUri());
        return new OAuth2UserRequest(registration,
                                     userRequest.getAccessToken(),
                                     userRequest.getAdditionalParameters());
    }

    private String getPermissionRoute(final String restUrl) {
        try {
            final URL targetUrl = new URL(new URL(restUrl),
                                          String.format(PERMISSIONS_INFO_URI,
                                                        URLEncoder.encode(String.format("%s=%s",
                                                                                        PERMISSION_FILTER_OBJECT_TYPE,
                                                                                        INTERACTION_STUDIO_APP),
                                                                          StandardCharsets.UTF_8.toString())));
            return targetUrl.toString();
        } catch (final MalformedURLException | UnsupportedEncodingException ex) {
            return null;
        }
    }
}


