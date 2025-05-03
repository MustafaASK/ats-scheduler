package com.ask.ats.handler;

import com.ask.ats.model.GenericResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.client.RestClientException;

import static com.ask.ats.utils.CommonUtils.buildGenericResponse;

/**
 * The type Global exception handler.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    /**
     * Handle rest client exception response entity.
     *
     * @param <T>     the type parameter
     * @param ex      the ex
     * @param message the message
     * @return the response entity
     */
    public <T> ResponseEntity<GenericResponse<T>> handleRestClientException(RestClientException ex, String message) {
        log.warn("Rest Api Error - {}, Message: {}", message, ex.getLocalizedMessage(), ex);
        return buildGenericResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, ex.getLocalizedMessage(), message,
                Boolean.FALSE);
    }

    /**
     * Handle json exception response entity.
     *
     * @param <T>     the type parameter
     * @param ex      the ex
     * @param message the message
     * @return the response entity
     */
    public <T> ResponseEntity<GenericResponse<T>> handleJsonException(JsonProcessingException ex, String message) {
        log.warn("Json Processing Error - {}, Message: {}", message, ex.getLocalizedMessage(), ex);
        return buildGenericResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, ex.getLocalizedMessage(), message,
                Boolean.FALSE);
    }


    /**
     * Handle general exception response entity.
     *
     * @param <T>     the type parameter
     * @param ex      the ex
     * @param message the message
     * @return the response entity
     */
    public <T> ResponseEntity<GenericResponse<T>> handleGeneralException(Exception ex, String message) {
        log.error("Unexpected Error - {}, Message: {}", message, ex.getLocalizedMessage(), ex);
        return buildGenericResponse(HttpStatus.INTERNAL_SERVER_ERROR, null, ex.getLocalizedMessage(), message,
                Boolean.FALSE);
    }

}