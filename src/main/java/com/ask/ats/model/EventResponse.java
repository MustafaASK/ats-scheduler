package com.ask.ats.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * The type Event response.
 */
@Data
@Builder
public class EventResponse {
    private Integer requestId;
    private List<Event> events;

    /**
     * The type Event.
     */
    @Data
    @Builder
    public static class Event {
        private String eventId;
        private String eventType;
        private long eventTimestamp;
        private String entityName;
        private int entityId;
        private String entityEventType;
        private EventMetadata eventMetadata;
        private List<String> updatedProperties;
    }

    /**
     * The type Event metadata.
     */
    @Data
    @Builder
    public static class EventMetadata {
        private String deletedIds;
        private String addedIds;
    }
}
