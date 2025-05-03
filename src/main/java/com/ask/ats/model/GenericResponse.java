package com.ask.ats.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * The type Generic response.
 *
 * @param <T> the type parameter
 */
@Data
@Builder
public class GenericResponse<T> {

    @JsonProperty(value = "Success")
    private Boolean success;

    @JsonProperty(value = "Error")
    private Boolean error;

    @JsonProperty(value = "Status")
    private int status;

    @JsonProperty(value = "Message")
    private String message;

    @JsonProperty(value = "TimeStamp")
    private String timeStamp;

    @JsonProperty(value = "ClientResponse")
    private String clientResponse;

    private T data;

}
