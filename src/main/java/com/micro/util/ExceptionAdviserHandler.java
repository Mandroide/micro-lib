package com.micro.util;

import com.micro.model.DefaultAPIError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor
@Slf4j
public class ExceptionAdviserHandler {

    public static ResponseEntity<DefaultAPIError> methodArgumentNotValidException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        StringBuilder errors = new StringBuilder();
        for (int i = 0; i < fieldErrors.size(); i++) {
            FieldError fieldError = fieldErrors.get(i);
            errors.append(fieldError.getField())
                    .append("::")
                    .append(fieldError.getDefaultMessage());
            if (i < fieldErrors.size() - 1) {
                errors.append(", ");
            }
        }
        List<ObjectError> globalErrors = bindingResult.getGlobalErrors();
        for (int i = 0; i < globalErrors.size(); i++) {
            ObjectError objectError = globalErrors.get(i);
            errors.append(objectError.getDefaultMessage());
            if (i < globalErrors.size() - 1) {
                errors.append(", ");
            }
        }
        return createResponse(errors.toString(), HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<DefaultAPIError> constraintViolationException(ConstraintViolationException ex) {
        Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();
        StringBuilder errors = new StringBuilder();
        int i = 0;
        for (ConstraintViolation<?> constraintViolation : constraintViolations) {
            String message = constraintViolation.getMessage();
            Path propertyPath = constraintViolation.getPropertyPath();
            int index = propertyPath.toString().lastIndexOf('.');
            errors.append(propertyPath.toString().substring(index + 1))
                    .append("::")
                    .append(message);
            if (i++ < constraintViolations.size() - 1) {
                errors.append(", ");
            }
        }
        return createResponse(errors.toString(), HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<DefaultAPIError> handlerMethodValidationException(HandlerMethodValidationException ex) {
        List<ParameterValidationResult> allValidationResults = ex.getAllValidationResults();
        StringBuilder errors = new StringBuilder();
        int i = 0;
        for (ParameterValidationResult validationResult : allValidationResults) {
            for (MessageSourceResolvable resolvableError : validationResult.getResolvableErrors()) {
                String message = resolvableError.getDefaultMessage();
                Object[] arguments = Objects.requireNonNull(resolvableError.getArguments());
                DefaultMessageSourceResolvable parameter = (DefaultMessageSourceResolvable) arguments[0];
                String parameterName = parameter.getDefaultMessage();
                errors.append(parameterName).append("::").append(message);
                if (i++ < arguments.length - 1) {
                    errors.append(", ");
                }
            }
        }
        return createResponse(errors.toString(), HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<DefaultAPIError> createResponse(String message, HttpStatus status) {
        return createResponse(null, null, message, status);
    }

    public static ResponseEntity<DefaultAPIError> createResponse(UUID tid, String owner, String message, HttpStatus status) {
        DefaultAPIError apiError = DefaultAPIError.builder()
                .tid(tid)
                .owner(owner)
                .message(message)
                .status(status.value())
                .build();
        log.info("Creating error :: {}", apiError);
        return ResponseEntity.status(status).body(apiError);
    }
}
