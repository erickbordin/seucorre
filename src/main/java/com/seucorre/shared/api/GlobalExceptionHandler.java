package com.seucorre.shared.api;

import jakarta.validation.ConstraintViolationException;
import com.seucorre.shared.exception.BusinessRuleException;
import com.seucorre.shared.exception.EntityNotFoundException;
import com.seucorre.shared.exception.IAUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRuleException(BusinessRuleException ex) {
        LOGGER.warn("Business rule error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException ex) {
        LOGGER.warn("Entity not found: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IAUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleIAUnavailableException(IAUnavailableException ex) {
        LOGGER.error("IA unavailable", ex);
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de IA temporariamente indisponível. Tente novamente mais tarde.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        LOGGER.warn("Illegal argument: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        LOGGER.warn("Validation error", ex);
        List<String> erros = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> resolveValidationMessage(fieldError))
                .toList();

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Erro de validação nos campos", erros);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        LOGGER.warn("Constraint violation", ex);
        List<String> erros = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .filter(message -> message != null && !message.isBlank())
                .toList();

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Erro de validação nos parâmetros", erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        LOGGER.warn("Malformed request body", ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        LOGGER.error("Unhandled server error", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor.");
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.erro(message));
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(HttpStatus status, String message, List<String> errors) {
        return ResponseEntity.status(status).body(ApiResponse.erro(message, errors == null || errors.isEmpty() ? null : errors));
    }

    private String resolveValidationMessage(FieldError fieldError) {
        if (fieldError.getDefaultMessage() != null && !fieldError.getDefaultMessage().isBlank()) {
            return fieldError.getDefaultMessage();
        }
        return "Campo inválido: " + fieldError.getField();
    }
}
