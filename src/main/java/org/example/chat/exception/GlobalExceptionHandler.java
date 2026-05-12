package org.example.chat.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameExists(UsernameAlreadyExistsException ex,
                                                                    HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler({UserNotRegisteredException.class, InvalidCredentialsException.class})
    public ResponseEntity<Map<String, Object>> handleAuth(RuntimeException ex, HttpServletRequest req) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid username or password", req);
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<Map<String, Object>> handleTokenRefresh(TokenRefreshException ex,
                                                                  HttpServletRequest req) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
                                                                  HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    @ExceptionHandler({EntityNotFoundException.class, UsernameNotFoundException.class,
            NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex,
                                                              HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex,
                                                                HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflictState(IllegalStateException ex,
                                                                   HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                   HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, "Resource already exists or violates a uniqueness constraint", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest req) {
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));
        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, "Validation failed", req);
        body.put("fieldErrors", fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), req);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(baseBody(status, message, req));
    }

    private Map<String, Object> baseBody(HttpStatus status, String message, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", req.getRequestURI());
        return body;
    }
}
