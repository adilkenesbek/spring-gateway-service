package kz.pet.eon.service.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import kz.pet.eon.handler.domain.ApiError;
import kz.pet.eon.handler.domain.ErrorMessage;
import kz.pet.eon.handler.exception.UnauthorizedException;
import kz.pet.eon.service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.SneakyThrows;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Objects;


@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return this::authenticationFilter;
//        return ((exchange, chain) -> Mono.empty());
    }

    /**
     * Метод для аутентификации запросов через Gateway
     *
     */
    private Mono<Void> authenticationFilter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (validator.isSecured.test(exchange.getRequest())) {
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return handleError(exchange,
                        new UnauthorizedException(ErrorMessage.EMPTY_AUTH_HEADER),
                        HttpStatus.UNAUTHORIZED, ErrorMessage.EMPTY_AUTH_HEADER);
            }

            String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if (Objects.nonNull(authHeader) && authHeader.startsWith("Bearer ")) {
                authHeader = authHeader.substring(7);
            }

            //Validating token
            Claims jwt;
            try {
                jwt = jwtUtil.validateToken(authHeader);
            } catch (ExpiredJwtException ex) {
                return handleError(exchange, ex, HttpStatus.UNAUTHORIZED, ErrorMessage.TOKEN_EXPIRED);
            } catch (JwtException ex) {
                return handleError(exchange, ex, HttpStatus.UNAUTHORIZED, ErrorMessage.INVALID_TOKEN);
            }

            // Validating authorized endpoints
            if (RouteValidator.authorizedApiEndpoints.containsKey(exchange.getRequest().getPath().toString())) {
                boolean authorized = false;
                for (String role : jwtUtil.getAuthorities(jwt)) {
                    if (RouteValidator.authorizedApiEndpoints
                            .get(exchange.getRequest().getPath().toString())
                            .contains(role)) {
                        authorized = true;
                        break;
                    }
                }

                if (!authorized)
                    return handleError(exchange, new RuntimeException(ErrorMessage.AUTHORIZATION_ERROR), HttpStatus.FORBIDDEN, ErrorMessage.AUTHORIZATION_ERROR);
            }
        }
        return chain.filter(exchange);
    }



    @SneakyThrows
    private DataBuffer createDataBuffer(Object value) {
        return new DefaultDataBufferFactory().wrap(objectMapper.writeValueAsBytes(value));
    }

    private Mono<Void> handleError(ServerWebExchange exchange,
                                   RuntimeException exc,
                                   HttpStatus httpStatus,
                                   String errorMessage) {
        exchange.getAttributes().putIfAbsent(ErrorAttributes.ERROR_ATTRIBUTE, exc);
        ApiError error = ApiError.builder()
                .status(httpStatus.value())
                .message(errorMessage)
                .timestamp(new Date())
                .details(exc.getLocalizedMessage())
                .path(exchange.getRequest().getURI().getPath())
                .build();
        DataBuffer dataBuffer = createDataBuffer(error);
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

    public static class Config {
    }
}
