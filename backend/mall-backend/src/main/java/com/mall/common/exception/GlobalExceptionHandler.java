package com.mall.common.exception;

import com.mall.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleBindException(WebExchangeBindException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return Mono.just(ResponseEntity.badRequest().body(ApiResponse.fail(400, message)));
    }

    @ExceptionHandler({ServerWebInputException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public Mono<ResponseEntity<ApiResponse<Void>>> handleParameterException(Exception ex) {
        return Mono.just(ResponseEntity.badRequest().body(ApiResponse.fail(400, extractMessage(ex))));
    }

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleBusinessException(BusinessException ex) {
        return Mono.just(ResponseEntity.ok(ApiResponse.fail(ex.getCode(), ex.getMessage())));
    }

    @ExceptionHandler({UnauthorizedException.class})
    public Mono<ResponseEntity<ApiResponse<Void>>> handleUnauthorizedException(UnauthorizedException ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(401, ex.getMessage())));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleResponseStatusException(ResponseStatusException ex) {
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(401, "unauthorized")));
        }
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus resolved = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
        return Mono.just(ResponseEntity.status(resolved)
                .body(ApiResponse.fail(resolved.value(), extractMessage(ex))));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleSystemException(Exception ex) {
        log.error("Unhandled system exception", ex);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(500, "internal server error")));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    private String extractMessage(Exception ex) {
        String message = ex.getMessage();
        return (message == null || message.isBlank()) ? "request parameter invalid" : message;
    }
}
