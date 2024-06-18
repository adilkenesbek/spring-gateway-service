package kz.pet.eon.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.pet.eon.handler.domain.ApiError;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ObjectMapper objectMapper;
    @SneakyThrows
    private Mono<Void> handleError(ServerWebExchange exchange, Exception exc, HttpStatus httpStatus) {
//        exchange.getAttributes().putIfAbsent(ErrorAttributes.ERROR_ATTRIBUTE, exc);
        ApiError error = ApiError.builder()
                .status(httpStatus.value())
                .message(httpStatus.name())
                .timestamp(new Date())
                .details(exc.getMessage())
                .path(exchange.getRequest().getURI().getPath())
                .build();
        DataBuffer dataBuffer = createDataBuffer(error);
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
    }
    @SneakyThrows
    private DataBuffer createDataBuffer(Object value) {
        return new DefaultDataBufferFactory().wrap(objectMapper.writeValueAsBytes(value));
    }
}
