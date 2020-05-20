package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class RestUserPermissions {

    private static String INTERACTION_STUDIO_APP = "InteractionStudioV2";
    private static String PERMISSION_FILTER_OBJECT_TYPE = "ObjectType";
    private static String PERMISSIONS_INFO_URI = "/platform-internal/v1/users/@current/permissions?$pageSize=1000&$filter=%s";

    @Autowired
    private RestTemplate rest;

    public List<McPermissionItem> permissions(final String restUrl, final String authToken)
            throws MalformedURLException, UnsupportedEncodingException {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(authToken);

        final HttpEntity<String> entity = new HttpEntity<>(headers);

        final URL targetUrl = new URL(new URL(restUrl), String.format(PERMISSIONS_INFO_URI,
             URLEncoder.encode(String.format("%s=%s", PERMISSION_FILTER_OBJECT_TYPE, INTERACTION_STUDIO_APP),
                               StandardCharsets.UTF_8.toString())));

        final ResponseEntity<McPermissionsResponse> exchange = rest.exchange(
                targetUrl.toString(),
                HttpMethod.GET,
                entity,
                McPermissionsResponse.class);

        return Collections.unmodifiableList(Objects.requireNonNull(exchange.getBody()).items);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McPermissionsResponse {
        public int count;
        public int page;
        public int pageSize;
        public ObjectNode links;
        public List<McPermissionItem> items;
    }

    public static class McPermissionItem {
        public boolean hasAccess;
        public String objectTypeName;
        public String operationName;
        public String name;
        public int id;
        public int mIds;

        @Override
        public String toString() {
            return "Item{" +
                    "hasAccess=" + hasAccess +
                    ", objectTypeName='" + objectTypeName + '\'' +
                    ", operationName='" + operationName + '\'' +
                    ", name='" + name + '\'' +
                    ", id=" + id +
                    ", mIds=" + mIds +
                    "}\n";
        }
    }
}
