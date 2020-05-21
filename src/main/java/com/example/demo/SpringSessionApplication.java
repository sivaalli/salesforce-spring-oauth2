package com.example.demo;

import com.example.demo.RestUserPermissions.McPermissionItem;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.McOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.session.FlushMode;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@SpringBootApplication
@EnableRedisHttpSession(flushMode = FlushMode.IMMEDIATE)
@Slf4j
public class SpringSessionApplication {
    private static final Logger logger = LoggerFactory.getLogger(SpringSessionApplication.class);

    private static final String USER_INFO_URI = "https://%s.auth-qa1.marketingcloudqaapis.com/v2/userinfo";

//    @Autowired
//    private RestUserPermissions userPermissionsClient;

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

        final Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("token", authorizedClient.getAccessToken().getTokenValue());

//        try {
//            final List<McPermissionItem> userPermissions =
//                    userPermissionsClient
//                            .permissions(getMcRestUrl(), authorizedClient.getAccessToken().getTokenValue());
//            attributes.put("userPermissions", userPermissions);
//        } catch (MalformedURLException | UnsupportedEncodingException ex) {
//            attributes.put("userPermissions", "ERROR: " + ex.getMessage());
//        }

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

//            private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
            private final McOAuth2UserService delegate = new McOAuth2UserService();

            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
                final String restUrl = userRequest.getAdditionalParameters().get("rest_instance_url").toString();
                final String tssd = restUrl.substring(8, 8 + 28); // TSSD is 28 character length.

//                final ClientRegistration registration =
//                        ClientRegistration.withClientRegistration(userRequest.getClientRegistration())
//                                .userInfoUri(String.format(USER_INFO_URI, tssd))
//                                .build();
//                logger.info("UserInfo route is {}", registration.getProviderDetails().getUserInfoEndpoint().getUri());
//                final OAuth2UserRequest request = new OAuth2UserRequest(registration,
//                                                                        userRequest.getAccessToken(),
//                                                                        userRequest.getAdditionalParameters());

                final OAuth2UserRequest oauth2UserRequest = createRequest(userRequest,
                                                                          String.format(USER_INFO_URI, tssd),
                                                                          "User");
                final OAuth2UserRequest oauth2PermissionRequest = createRequest(userRequest,
                                                                                getPermissionRoute(restUrl),
                                                                                "Permission");

//                final OAuth2User user = delegate.loadUser(request);
                final OAuth2User user = delegate.loadUser(oauth2UserRequest, oauth2PermissionRequest);

                //TODO: Need to find an alternate way of getting rest template to work. Look into DefaultOAuth2UserService
//                if (StringUtils.hasText(registration.getProviderDetails().getUserInfoEndpoint().getUri())) {
//                    final Map<String, Object> attributes = new HashMap<>(user.getAttributes());
//                    final String userNameAttributeName = registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
//
//                    try {
//                        final List<McPermissionItem> userPermissions =
//                                userPermissionsClient
//                                        .permissions(restUrl, request.getAccessToken().getTokenValue());
//                        attributes.put("userPermissionsInOAuth", userPermissions);
//                    } catch (MalformedURLException | UnsupportedEncodingException ex) {
//                        attributes.put("userPermissionsInOAuth", "ERROR: " + ex.getMessage());
//                    }
//
//                    return new DefaultOAuth2User(user.getAuthorities(), attributes, userNameAttributeName);
//                }

                return user;
            }
        };
    }

    @Bean
    public RestTemplate template(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(5))
                // use apache http client
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .build();
    }

//    public static String getMcRestUrl() {
//        final OAuth2AuthenticationToken authentication =
//                (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
//        Object restUrlInfo = null;
//        if (authentication != null &&
//                authentication.getPrincipal() != null &&
//                authentication.getPrincipal().getAttributes() != null &&
//                authentication.getPrincipal().getAttributes().containsKey("rest")) {
//            restUrlInfo = authentication.getPrincipal().getAttribute("rest");
//        }
//        String restTssdUrl = "";
//        if (restUrlInfo instanceof LinkedHashMap) {
//            final LinkedHashMap restUrlInfoMap = (LinkedHashMap)restUrlInfo;
//            if (restUrlInfoMap.containsKey("rest_instance_url")) {
//                restTssdUrl = restUrlInfoMap.get("rest_instance_url").toString();
//            }
//        }
//        logger.info("Rest Tssd Url: " + restTssdUrl);
//        return restTssdUrl;
//    }

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
        final String INTERACTION_STUDIO_APP = "InteractionStudioV2";
        final String PERMISSION_FILTER_OBJECT_TYPE = "ObjectType";
        final String PERMISSIONS_INFO_URI = "/platform-internal/v1/users/@current/permissions?$pageSize=1000&$filter=%s";

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


