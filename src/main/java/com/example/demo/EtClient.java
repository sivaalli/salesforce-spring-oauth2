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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class EtClient {

    @Autowired
    private RestTemplate rest;

    public List<Item> permissions(String authToken) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(authToken);

        final HttpEntity<String> entity = new HttpEntity<>(headers);

        final ResponseEntity<McResponse> exchange = rest.exchange(
                "/platform-internal/v1/users/@current/permissions?$pageSize=1000",
                HttpMethod.GET,
                entity,
                McResponse.class);

        return Collections.unmodifiableList(Objects.requireNonNull(exchange.getBody()).items);

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class McResponse {
        public int count;
        public int page;
        public int pageSize;
        public ObjectNode links;
        public List<Item> items;
    }

    public static class Item {
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


