package com.ask.ats.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Sns event.
 */
@Data
@Builder
public class SnsEvent {
    private String entityName;
    private String entityId;
    private String eventType;
    private int clientId;
    private String data;
    private String updatedFields;
    private EventMetadata metadata;
    private String atsName;
    private int recruiterId;

    /**
     * The type Event metadata.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMetadata {
        private String deletedIds;
        private String addedIds;
    }
}
