package com.example.gateway.service.filter;

import com.example.gateway.service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.example.gateway.service.util.JwtUtil.getAuthorities;
import static com.example.gateway.service.util.JwtUtil.validateToken;


@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil) {
        super(Config.class);
        this.validator = validator;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (validator.isSecured.test(exchange.getRequest())) {
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    throw new RuntimeException("Missing authorization header!");
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (Objects.nonNull(authHeader) && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                }

                //Validating token
                Claims jwt = validateToken(authHeader);
                if (Objects.isNull(jwt)) {
                    throw new IllegalArgumentException("Invalid token validation!");
                } else {
                    // Validating authorized endpoints
                    if (RouteValidator.authorizedApiEndpoints.containsKey(exchange.getRequest().getPath().toString())) {
                        boolean authorized = false;
                        for (String role : getAuthorities(jwt)) {
                            if (RouteValidator.authorizedApiEndpoints
                                    .get(exchange.getRequest().getPath().toString())
                                    .contains(role)) {
                                authorized = true;
                                break;
                            }
                        }

                        if (!authorized) throw new RuntimeException("Forbidden!");
                    }
                }
            }
            return chain.filter(exchange);
        });
    }

    public static class Config {
    }
}
