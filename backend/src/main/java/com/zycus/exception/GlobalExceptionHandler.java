package com.zycus.exception;

import com.zycus.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log=LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse notFound(ResourceNotFoundException e,HttpServletRequest req) {
        return response(HttpStatus.NOT_FOUND,e.getMessage(),req);
    }

    @ExceptionHandler({InvalidSuggestionException.class,NoAvailableAgentException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse conflict(Exception e,HttpServletRequest req) {
        return response(HttpStatus.CONFLICT,e.getMessage(),req);
    }

    @ExceptionHandler({IllegalArgumentException.class,HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(Exception e,HttpServletRequest req) {
        return response(HttpStatus.BAD_REQUEST,"Invalid request",req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse validation(MethodArgumentNotValidException e,HttpServletRequest req) {
        String msg=e.getBindingResult().getFieldErrors().stream()
                .map(field->field.getField()+": "+field.getDefaultMessage())
                .reduce((a,b)->a+"; "+b).orElse("Validation error");
        return response(HttpStatus.BAD_REQUEST,msg,req);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse unexpected(Exception e,HttpServletRequest req) {
        log.error("Unhandled API failure",e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,"Unexpected error",req);
    }

    private ErrorResponse response(HttpStatus status,String message,HttpServletRequest req) {
        return new ErrorResponse(LocalDateTime.now(),status.value(),status.getReasonPhrase(),
                message,req.getRequestURI());
    }
}
