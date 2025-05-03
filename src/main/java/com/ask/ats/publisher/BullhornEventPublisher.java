package com.ask.ats.publisher;

import com.ask.ats.config.ScheduledTaskConfig;
import com.ask.ats.model.*;
import com.ask.ats.service.ClientService;
import com.ask.ats.utils.CommonUtils;
import com.ask.ats.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishBatchRequest;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;
import software.amazon.awssdk.services.sns.model.PublishBatchResponse;

import java.util.*;
import java.util.stream.Collectors;

import static com.ask.ats.utils.Constants.BULLHORN;


/**
 * The type Async event processor service.
 */
@Slf4j
@Service
public class BullhornEventPublisher {

    private final ClientService clientService;
    private final Map<String, List<Integer>> entityIdMap = new HashMap<>();
    private Integer maxId = 0;
    private final Map<String, Integer> maxIdMap = new HashMap<>();
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sns.bullhornTopicArn}")
    private String snsTopicArn;


    public BullhornEventPublisher(@Lazy ClientService clientService, NamedParameterJdbcTemplate jdbcTemplate,
                                  SnsClient snsClient, ObjectMapper objectMapper) {
        this.clientService = clientService;
        this.jdbcTemplate = jdbcTemplate;
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }



    public void processEventsAsync(List<EventResponse.Event> events, int clientId) {
        log.info("Asynchronously processing {} events", events.size());
        String query = "SELECT ua.autoid AS id, ua.atsvalue AS entityId, 'Candidate' AS entityName "
                + "FROM DB_NAME.dbo.user_ats ua WHERE ua.autoid > :userMaxId AND ua.atsname = :atsName "
                + "UNION ALL "
                + "SELECT ja.autoid AS id, ja.atsvalue AS entityId, 'JobOrder' AS entityName "
                + "FROM DB_NAME.dbo.job_ats ja WHERE ja.autoid > :jobMaxId AND ja.atsname = :atsName "
                + "UNION ALL "
                + "SELECT ta.autoid AS id, ta.atsvalue AS entityId, 'Tearsheet' AS entityName "
                + "FROM DB_NAME.dbo.talentpool_ats ta WHERE ta.autoid > :poolMaxId AND ta.atsname = :atsName "
                + "UNION ALL "
                + "SELECT oa.autoid AS id, oa.atsvalue AS entityId, 'JobSubmission' AS entityName "
                + "FROM DB_NAME.dbo.openresumes_ats oa WHERE oa.autoid > :openMaxId AND oa.atsname = :atsName";

        MapSqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("userMaxId", maxIdMap.getOrDefault("userMaxId", 0))
                .addValue("jobMaxId", maxIdMap.getOrDefault("jobMaxId", 0))
                .addValue("poolMaxId", maxIdMap.getOrDefault("poolMaxId", 0))
                .addValue("openMaxId", maxIdMap.getOrDefault("openMaxId", 0))
                .addValue("atsName", BULLHORN);
        jdbcTemplate.query(query.replace("DB_NAME", "Curately_" + clientId), parameterSource,
                (rs, rowNum) -> {
                    String entityName = rs.getString("entityName").trim();
                    int id = rs.getInt("id");
                    int entityId = rs.getInt("entityId");
                    String key = getKeyByEntityName(entityName);
                    maxIdMap.put(key, Math.max(maxIdMap.getOrDefault(key, 0), id));
                    entityIdMap.computeIfAbsent(entityName, k -> new ArrayList<>()).add(entityId);
                    return null;
                }
        );

        Map<String, List<EventResponse.Event>> groupedEventsByName = events.stream()
                .collect(Collectors.groupingBy(EventResponse.Event::getEntityName));

        groupedEventsByName.forEach((entityName, eventsByName) -> {
            Map<String, List<String>> entityIdFieldsMap = eventsByName.stream()
                    .collect(Collectors.groupingBy(EventResponse.Event::getEntityId))
                    .values().stream()
                    .map(eventList -> buildEntityIdFieldsMap(eventList, entityName))
                    .collect(Collectors.toMap(
                            event -> event.getEntityId() + "-" + event.getEntityEventType(),
                            event -> {
                                List<String> updatedProperties = event.getUpdatedProperties();
                                return updatedProperties != null
                                        ? updatedProperties
                                        : CommonUtils.getAllFieldsAsList(event.getEntityName());
                            }
                    ));
            Map<String, Map<String, List<Integer>>> entityIdMetadataMap = eventsByName.stream()
                    .collect(Collectors.groupingBy(EventResponse.Event::getEntityId))
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getValue().get(0).getEntityId() + "-"
                                    + entry.getValue().get(0).getEntityEventType(),
                            entry -> buildAddedOrRemovedIdMap(entry.getValue())
                    ));
            processEventEntity(entityIdFieldsMap, entityName, clientId, entityIdMetadataMap);
        });
    }

    private String getKeyByEntityName(String entityName) {
        switch (entityName) {
            case Constants.CANDIDATE -> {
                return "userMaxId";
            }
            case Constants.JOB_ORDER -> {
                return "jobMaxId";
            }
            case Constants.TEARSHEET -> {
                return "poolMaxId";
            }
            case Constants.SUBMISSION -> {
                return "openMaxId";
            }
            default -> throw new IllegalStateException("Unexpected value: " + entityName);
        }
    }

    private EventResponse.Event buildEntityIdFieldsMap(List<EventResponse.Event> events, String entityName) {
        if (events.size() == 1) {
            return events.get(0);
        }

        Set<String> combinedFields = events.stream()
                .filter(event -> "UPDATED".equalsIgnoreCase(event.getEntityEventType()))
                .flatMap(event -> event.getUpdatedProperties().stream())
                .collect(Collectors.toSet());

        return events.stream()
                .filter(event -> "INSERTED".equalsIgnoreCase(event.getEntityEventType()))
                .findFirst()
                .map(event -> {
                    event.setUpdatedProperties(List.of(CommonUtils.getAllFields(entityName).split(",")));
                    return event;
                })
                .orElseGet(() -> {
                    events.get(0).setUpdatedProperties(new ArrayList<>(combinedFields));
                    return events.get(0);
                });
    }


    private Map<String, List<Integer>> buildAddedOrRemovedIdMap(List<EventResponse.Event> events) {
        Set<Integer> addedIdsSet = new HashSet<>();
        Set<Integer> removedIdsSet = new HashSet<>();

        for (EventResponse.Event event : events) {
            EventResponse.EventMetadata metadata = event.getEventMetadata();
            if (metadata != null) {
                Optional.ofNullable(metadata.getAddedIds()).ifPresent(ids ->
                        addedIdsSet.addAll(Arrays.stream(ids.split(",")).map(Integer::parseInt).toList()));
                Optional.ofNullable(metadata.getDeletedIds()).ifPresent(ids ->
                        removedIdsSet.addAll(Arrays.stream(ids.split(",")).map(Integer::parseInt).toList()));
            }
        }

        Map<String, List<Integer>> resultMap = new HashMap<>();
        resultMap.put("addedIds", addedIdsSet.stream().toList());
        resultMap.put("removedIds", removedIdsSet.stream().toList());
        return resultMap;
    }

    /**
     * Process event entity response entity.
     *
     * @param <T>                 the type parameter
     * @param entityIdFieldsMap   the entity id fields map
     * @param entityName          the entity name
     * @param clientId            the client id
     * @param entityIdMetadataMap the entity id metadata map
     * @return the response entity
     */
    public <T> ResponseEntity<GenericResponse<Object>> processEventEntity(
            Map<String, List<String>> entityIdFieldsMap, String entityName, int clientId,
            Map<String, Map<String, List<Integer>>> entityIdMetadataMap) {
        if (entityIdFieldsMap.isEmpty()) {
            log.error("Ats values are empty in the request for {}, client {}", entityName, clientId);
            return CommonUtils.buildCuratelyResponse(HttpStatus.BAD_REQUEST, null, null,
                    "Entity ids map is mandatory to process", Boolean.FALSE);
        }

        Map<Integer, String> entityIdEventTypeMap = entityIdFieldsMap.keySet().stream()
                .map(key -> key.split("-"))
                .collect(Collectors.toMap(
                        parts -> Integer.parseInt(parts[0]),
                        parts -> parts[1]
                ));

        List<String> entityIds = entityIdEventTypeMap.keySet().stream()
                .sorted((a, b) -> b - a)
                .map(String::valueOf)
                .toList();
        log.info("Started processEventEntity for {} {}, client {}", entityName, entityIds, clientId);

        String fields = CommonUtils.getAllFields(entityName);
        if (entityName.equalsIgnoreCase(Constants.SUBMISSION)) {
            fields = fields.replace("candidate", "candidate(%s)"
                    .formatted(CommonUtils.getAllFields(Constants.CANDIDATE)));
            fields = fields.replace("jobOrder", "jobOrder(%s)"
                    .formatted(CommonUtils.getAllFields(Constants.JOB_ORDER)));
        }
        ResponseEntity<GenericResponse<T>> responseEntity = clientService.getMultipleEntities(
                String.join(",", entityIds), entityName, clientId, fields,
                entityIdFieldsMap.size(), 0);
        if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.hasBody()) {
            GenericResponse<T> body = responseEntity.getBody();
            if (body != null && body.getData() != null) {
                Boolean isSuccess = processEvents(entityIdFieldsMap, entityName, clientId, body.getData(),
                        entityIdEventTypeMap, entityIdMetadataMap);
                if (Boolean.TRUE.equals(isSuccess)) {
                    log.info("Success publishing messages of {}, client {} to Bullhorn sns topic", entityName,
                            clientId);
                    return CommonUtils.buildCuratelyResponse(HttpStatus.OK, null,
                            null, "Fetching entities from Bullhorn to Curately asynchronously",
                            Boolean.TRUE);
                }
            }
        }
        log.info("Error publishing messages of {}, client {} to Bullhorn sns topic", entityName, clientId);
        return CommonUtils.buildCuratelyResponse(HttpStatus.INTERNAL_SERVER_ERROR, null,
                responseEntity.toString(), "Failed to pull entities from Bullhorn to Curately", Boolean.FALSE);
    }

    private <T> Boolean processEvents(Map<String, List<String>> entityIdFieldsMap, String entityName,
                                      int clientId, T data, Map<Integer, String> entityIdEventTypeMap,
                                      Map<String, Map<String, List<Integer>>> entityIdMetadataMap) {
        log.info("Started publishing {} {} entities for client {} as events to bullhorn sns topic", entityName,
                entityIdEventTypeMap.keySet(), clientId);
        Map<Integer, String> dataMap = new HashMap<>();
        int entityId = -1;
        try {
            if (data instanceof List<?>) {
                for (Object o : (List<?>) data) {
                    entityId = (int) ((LinkedHashMap<?, ?>) o).get("id");
                    dataMap.put(entityId, objectMapper.writeValueAsString(o));
                }
            } else {
                entityId = (int) ((LinkedHashMap<?, ?>) data).get("id");
                dataMap.put(entityId, objectMapper.writeValueAsString(data));
            }
        } catch (JsonProcessingException e) {
            log.error("Error converting {} entity {} data to json string", entityName, entityId, e);
        }

        if (dataMap.isEmpty()) {
            log.warn("Get multiple entities data is empty for {} entities {}", entityName, dataMap.keySet());
            return false;
        }

        List<PublishBatchRequestEntry> publishRequests = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : dataMap.entrySet()) {
            entityId = entry.getKey();
            String dataString = entry.getValue();
            String eventType = entityIdEventTypeMap.get(entityId);
            String key = entityId + "-" + eventType;
            List<String> updatedProperties = entityIdFieldsMap.get(key);
            SnsEvent.EventMetadata metadata = new SnsEvent.EventMetadata();

            if ("Tearsheet".equalsIgnoreCase(entityName)
                    && "UPDATED".equalsIgnoreCase(eventType)
                    && updatedProperties.contains("candidates")) {
                Map<String, Object> result = performTearSheetUpdateBuild(dataString, clientId,
                        entityIdMetadataMap.getOrDefault(key, CommonUtils.getRemovedOrAddedEmptyMap()), metadata);
                if (!(boolean) result.getOrDefault("success", false)) {
                    return false;
                }
                dataString = (String) result.getOrDefault("updatedData", dataString);
            }

            SnsEvent snsEvent = SnsEvent.builder()
                    .entityName(entityName)
                    .entityId(String.valueOf(entityId))
                    .eventType(eventType)
                    .clientId(clientId)
                    .data(dataString)
                    .updatedFields(String.join(",", updatedProperties))
                    .metadata(metadata)
                    .atsName(BULLHORN)
                    .build();

            String message = convertModelToString(snsEvent);
            if (message != null) {
                publishRequests.add(PublishBatchRequestEntry.builder()
                        .message(message)
                        .id(UUID.randomUUID().toString())
                        .build());
            }
            if (publishRequests.size() == 10) {
                publishBatch(publishRequests);
                publishRequests.clear();
                log.info("Published 10 events of {}, client {} to Bullhorn sns topic", entityName, clientId);
            }
        }

        if (!publishRequests.isEmpty()) {
            publishBatch(publishRequests);
            log.info("Published {} events of {}, client {} to Bullhorn sns topic", publishRequests.size(), entityName,
                    clientId);
        }
        return true;
    }

    /**
     * Perform tear sheet update build map.
     *
     * @param data     the data
     * @param clientId the client id
     * @param map      the map
     * @param metadata the metadata
     * @return the map
     */
    public Map<String, Object> performTearSheetUpdateBuild(String data, int clientId,
                                                           Map<String, List<Integer>> map,
                                                           SnsEvent.EventMetadata metadata) {

        log.info("Rebuilding tearsheet with candidates and jobs data started for client {}: {}", clientId, metadata);
        boolean success = false;
        try {
            Tearsheet tearsheet = objectMapper.readValue(data, Tearsheet.class);
            List<Integer> addedIds = map.getOrDefault("addedIds", Collections.emptyList());
            List<Integer> removedIds = map.getOrDefault("removedIds", Collections.emptyList());
            String fields = CommonUtils.getAllFields(Constants.CANDIDATE);

            if (!addedIds.isEmpty()) {
                metadata.setDeletedIds(StringUtils.join(removedIds, ","));
                Tearsheet updatedTearsheet = buildTearsheet(tearsheet, Collections.emptyList(), 0);
                data = objectMapper.writeValueAsString(updatedTearsheet);
            }

            if (!addedIds.isEmpty()) {
                metadata.setAddedIds(StringUtils.join(addedIds, ","));
                List<Candidate> candidates = processAddedIds(addedIds, clientId, fields);
                if (!candidates.isEmpty()) {
                    Tearsheet updatedTearsheet = buildTearsheet(tearsheet, candidates, candidates.size());
                    data = objectMapper.writeValueAsString(updatedTearsheet);
                }
            }
            success = true;
        } catch (JsonProcessingException e) {
            log.error("Error converting tearsheet object to json for client {}: {}", clientId, data, e);
        } catch (Exception e) {
            log.error("Unknown error while building tearsheet object for client {}: {}", clientId, data, e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("success", success);
        return result;
    }


    private List<Candidate> processAddedIds(List<Integer> addedIds, int clientId, String fields) {
        List<Candidate> candidates = new ArrayList<>();
        try {
            ResponseEntity<GenericResponse<Object>> responseEntity = clientService.getMultipleEntities(
                    StringUtils.join(addedIds, ","), Constants.CANDIDATE, clientId, fields,
                    addedIds.size(), 0);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.hasBody()) {
                GenericResponse<Object> body = responseEntity.getBody();
                if (body != null && body.getData() != null) {
                    if (body.getData() instanceof List) {
                        candidates = objectMapper.convertValue(body.getData(),
                                new TypeReference<List<Candidate>>() {
                                });
                    } else {
                        Candidate candidate = objectMapper.convertValue(body.getData(), Candidate.class);
                        candidates.add(candidate);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error performing entity service operation: {}", e.getMessage(), e);
        }
        return candidates;
    }

    private Tearsheet buildTearsheet(Tearsheet baseTearsheet, List<Candidate> candidates, int totalCandidates) {
        return Tearsheet.builder()
                .id(baseTearsheet.getId())
                .candidates(ToManyEntityAssociation.<Candidate>builder()
                        .data(candidates)
                        .total(totalCandidates)
                        .build())
                .clientContacts(baseTearsheet.getClientContacts())
                .dateAdded(baseTearsheet.getDateAdded())
                .dateLastModified(baseTearsheet.getDateLastModified())
                .description(baseTearsheet.getDescription())
                .isDeleted(baseTearsheet.getIsDeleted())
                .isPrivate(baseTearsheet.getIsPrivate())
                .jobOrders(baseTearsheet.getJobOrders())
                .name(baseTearsheet.getName())
                .owner(baseTearsheet.getOwner())
                .recipients(baseTearsheet.getRecipients())
                .build();
    }


    private void publishBatch(List<PublishBatchRequestEntry> publishBatchRequestEntries) {
        PublishBatchRequest publishRequest = PublishBatchRequest.builder()
                .topicArn(snsTopicArn)
                .publishBatchRequestEntries(publishBatchRequestEntries)
                .build();
        PublishBatchResponse publishResponse = snsClient.publishBatch(publishRequest);
        log.info("Published message successfully: {}", publishResponse.successful());
    }

    private String convertModelToString(SnsEvent snsEvent) {
        try {
            return objectMapper.writeValueAsString(snsEvent);
        } catch (JsonProcessingException e) {
            log.error("Error converting sns event model to json string", e);
        }
        return null;
    }


}
