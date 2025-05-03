package com.ask.ats.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * The type Bullhorn response.
 *
 * @param <T> the type parameter
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BullhornResponse<T> {

    private String changedEntityType;
    private Integer changedEntityId;
    private String changeType;
    private List<Message> messages;
    private T data;

    /**
     * The type Message.
     */
    @Data
    @Builder
    public static class Message {
        private String detailMessage;
        private String propertyName;
        private String severity;
        private String type;
    }

}
