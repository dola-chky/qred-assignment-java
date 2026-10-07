package com.dola.qredpaymentbackend.payment.web.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception,
      HttpServletRequest request) {
    List<String> details = exception.getBindingResult().getFieldErrors().stream()
        .map(this::formatFieldError)
        .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> handleBadRequest(BadRequestException exception,
      HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI(), List.of());
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiError> handleMissingRequestParameter(
      MissingServletRequestParameterException exception, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI(), List.of());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadableRequest(HttpMessageNotReadableException exception,
      HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Malformed request body", request.getRequestURI(), List.of());
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(ConflictException exception,
      HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI(), List.of());
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(NotFoundException exception,
      HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI(), List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception exception, HttpServletRequest request) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request.getRequestURI(),
        List.of(exception.getClass().getSimpleName()));
  }

  private String formatFieldError(FieldError error) {
    return error.getField() + ": " + error.getDefaultMessage();
  }

  private ResponseEntity<ApiError> build(HttpStatus status, String message, String path,
      List<String> details) {
    return ResponseEntity.status(status).body(
        new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, details));
  }
}


