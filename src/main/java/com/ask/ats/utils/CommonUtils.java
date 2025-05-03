package com.ask.ats.utils;

import com.ask.ats.model.GenericResponse;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * The type Common utils.
 */
@UtilityClass
@Slf4j
public class CommonUtils {


    /**
     * Gets all fields as list.
     *
     * @param entityName the entity name
     * @return the all fields as list
     */
    public static List<String> getAllFieldsAsList(String entityName) {
        try {
            Class<?> clazz = Class.forName("com.ask.ats.model." + entityName);
            return Stream.of(clazz.getDeclaredFields())
                    .map(Field::getName)
                    .toList();
        } catch (ClassNotFoundException e) {
            log.error("Model class for getting fields not found for entity: {}", entityName, e);
            return Collections.emptyList();
        }
    }


    /**
     * Gets all fields.
     *
     * @param entityName the entity name
     * @return the all fields
     */
    public static String getAllFields(String entityName) {
        try {
            Class<?> clazz = Class.forName("com.ask.ats.model." + entityName);
            return Stream.of(clazz.getDeclaredFields())
                    .map(Field::getName)
                    .collect(Collectors.joining(","));
        } catch (ClassNotFoundException e) {
            log.error("Model class for getting fields not found for entity: {}", entityName, e);
            return "";
        }
    }


    /**
     * Build generic response response entity.
     *
     * @param <T>            the type parameter
     * @param status         the status
     * @param body           the body
     * @param clientResponse the client response
     * @param message        the message
     * @param success        the success
     * @return the response entity
     */
    public static <T> ResponseEntity<GenericResponse<T>> buildGenericResponse(
            HttpStatusCode status, T body, String clientResponse, String message, boolean success) {
        return ResponseEntity.status(status).body(GenericResponse.<T>builder()
                .data(body)
                .status(status.value())
                .success(success)
                .error(!success)
                .timeStamp(LocalDateTime.now().toString())
                .message(message)
                .clientResponse(clientResponse)
                .build());
    }

    /**
     * Build curately response entity.
     *
     * @param <T>            the type parameter
     * @param status         the status
     * @param data           the data
     * @param clientResponse the client response
     * @param message        the message
     * @param success        the success
     * @return the response entity
     */
    public static <T> ResponseEntity<GenericResponse<T>> buildCuratelyResponse(
            HttpStatusCode status, T data, String clientResponse, String message, Boolean success) {
        return ResponseEntity.ok().body(GenericResponse.<T>builder()
                .data(data)
                .status(status.value())
                .success(success)
                .error(!success)
                .timeStamp(LocalDateTime.now().toString())
                .message(message)
                .clientResponse(clientResponse)
                .build());
    }


    /**
     * Gets removed or added empty map.
     *
     * @return the removed or added empty map
     */
    public static Map<String, List<Integer>> getRemovedOrAddedEmptyMap() {
        return Map.of("deleted", Collections.emptyList(), "notDeleted", Collections.emptyList());
    }


    /**
     * Is invalid response boolean.
     *
     * @param <T>  the type parameter
     * @param body the body
     * @return the boolean
     */
    public static <T> boolean isInvalidResponse(GenericResponse<List<T>> body) {
        return body == null || !body.getSuccess() || body.getData() == null;
    }

    /**
     * Handle error response.
     *
     * @param <T>        the type parameter
     * @param entityName the entity name
     * @param clientId   the client id
     * @param response   the response
     */
    public static <T> void handleErrorResponse(String entityName, int clientId,
                                         ResponseEntity<GenericResponse<List<T>>> response) {
        log.error("Invalid response from Job diva while fetching {}, client {} : {}", entityName, clientId, response);
    }

}
