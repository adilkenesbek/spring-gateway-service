package com.example.gateway.service.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> openApiEndpoints =
            List.of("/authentication/api/auth/login");
    public static final Map<String, List<String>> authorizedApiEndpoints =
            Map.of("/authentication/api/auth/**", List.of("ADMIN"));

    public Predicate<ServerHttpRequest> isSecured =
            serverHttpRequest ->
                    openApiEndpoints.stream().noneMatch(uri ->
                            serverHttpRequest.getURI().getPath().contains(uri));
}
