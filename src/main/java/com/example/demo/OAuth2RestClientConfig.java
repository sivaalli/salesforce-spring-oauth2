package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.util.StringUtils;

import java.time.Duration;

@Configuration
@Slf4j
public class OAuth2RestClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2RestClientConfig.class);

    private final RestTemplateBuilder restTemplateBuilder;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    public OAuth2RestClientConfig(RestTemplateBuilder restTemplateBuilder,
                                  OAuth2AuthorizedClientService authorizedClientService) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.authorizedClientService = authorizedClientService;
    }

    @Bean
    public RestTemplate restTemplate() {
        final OAuth2AuthenticationToken authentication =
                (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        final Object restUrlInfo = authentication.getPrincipal().getAttributes().containsKey("rest")
                ? authentication.getPrincipal().getAttribute("rest")
                : null;
        logger.info("Rest Url Info Authentication object type: " + restUrlInfo != null ? restUrlInfo.getClass().getName() : "was null");
        String restTssdUrl = "";
        if (!StringUtils.isEmpty(restTssdUrl)) {
            final JsonObject convertedObject = new Gson().fromJson(restUrlInfo.toString(), JsonObject.class);
            if (convertedObject.has("rest_instance_url")) {
                restTssdUrl = convertedObject.get("rest_instance_url").getAsString();
            }
        }

        return restTemplateBuilder.additionalInterceptors((httpRequest, bytes, clientHttpRequestExecution) -> {
            final OAuth2AuthorizedClient authedClient = authorizedClientService.loadAuthorizedClient(
                    authentication.getAuthorizedClientRegistrationId(),
                    authentication.getName());

            final String tokenType = authedClient.getAccessToken().getTokenType().getValue();
            final String token = authedClient.getAccessToken().getTokenValue();
            final String authHeader = String.format("%s %s", tokenType, token);

            log.debug("Intercepting HTTP request and adding OAuth2 authentication header");
            httpRequest.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);

            return clientHttpRequestExecution.execute(httpRequest, bytes);
        })
        .setConnectTimeout(Duration.ofSeconds(10))
        .setReadTimeout(Duration.ofSeconds(5))
        .rootUri(restTssdUrl)
        // use apache http client
        .requestFactory(HttpComponentsClientHttpRequestFactory.class)
        .build();
    }
}
