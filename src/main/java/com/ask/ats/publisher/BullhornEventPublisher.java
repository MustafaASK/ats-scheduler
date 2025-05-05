package com.ask.ats.publisher;

import com.ask.ats.model.*;
import com.ask.ats.service.ClientService;
import com.ask.ats.utils.CommonUtils;
import com.ask.ats.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishBatchRequest;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;
import software.amazon.awssdk.services.sns.model.PublishBatchResponse;

import static com.ask.ats.utils.Constants.*;


/**
 * The type Async event processor service.
 */
@Slf4j
@Service
public class BullhornEventPublisher {

    private final ClientService entityService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private static final String requiredFieldsConfig = "title,description,onSite";

    @Value("${aws.sns.bullhornTopicArn}")
    private String snsTopicArn;

    /**
     * Instantiates a new Async event processor service.
     *
     * @param entityService the entity service
     * @param jdbcTemplate  the jdbc template
     * @param snsClient     the sns client
     * @param objectMapper  the object mapper
     */
    public BullhornEventPublisher(@Lazy ClientService entityService, NamedParameterJdbcTemplate jdbcTemplate,
                                  SnsClient snsClient, ObjectMapper objectMapper) {
        this.entityService = entityService;
        this.jdbcTemplate = jdbcTemplate;
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Process events async.
     *
     * @param events   the events
     * @param clientId the client id
     */
    public void processEventsAsync(List<EventResponse.Event> events, int clientId, int recruiterId) {
        log.info("Asynchronously processing {} events", events.size());
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
                                if (updatedProperties != null) {
                                    Set<String> updatedSet = new HashSet<>(fetchUserProperties(entityName));
                                    updatedSet.addAll(updatedProperties);
                                    return new ArrayList<>(updatedSet);
                                }
                                return CommonUtils.getAllFieldsAsList(event.getEntityName());
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
            processEventEntity(entityIdFieldsMap, entityName, clientId, entityIdMetadataMap, Boolean.FALSE, recruiterId, Collections.emptyMap());
        });
    }

    private List<String> fetchUserProperties(String entityName) {
        return switch (entityName) {
            case JOB_ORDER -> List.of("assignedUsers", "owner");
            case CANDIDATE -> List.of("owner", "secondaryOwners");
            case SUBMISSION -> List.of("owners", "sendingUser");
            case TEARSHEET -> List.of("owner");
            default -> Collections.emptyList();
        };
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
    public <T> ValidationResult processEventEntity(
            Map<String, List<String>> entityIdFieldsMap, String entityName, int clientId,
            Map<String, Map<String, List<Integer>>> entityIdMetadataMap, boolean manualProcess, int recruiterId, Map<String, String> missingFields) {
        ValidationResult validationResult = null;
        if (entityIdFieldsMap.isEmpty()) {
            log.error("Ats values are empty in the request for {}, client {}", entityName, clientId);
            return validationResult;
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
        ResponseEntity<GenericResponse<T>> responseEntity = entityService.getMultipleEntities(
                String.join(",", entityIds), entityName, clientId, fields,
                entityIdFieldsMap.size(), 0);
        if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.hasBody()) {
            GenericResponse<T> body = responseEntity.getBody();
            if (body != null && body.getData() != null) {
                T payload = body.getData();
                if(entityName.equals(JOB_ORDER) && entityIds.size() == 1 && manualProcess){
                    JobOrder job = objectMapper.convertValue(payload, JobOrder.class);
                    validationResult = validateAndBuildJob(job, missingFields);
                    if (Boolean.FALSE.equals(validationResult.getIsValidJob())){
                        return validationResult;
                    }
                    LinkedHashMap<String,Object> jobMap = objectMapper.convertValue(
                            job,
                            new TypeReference<>() {
                            }
                    );
                    payload = (T) jobMap;
                }

                Boolean isSuccess = processEvents(entityIdFieldsMap, entityName, clientId, payload,
                        entityIdEventTypeMap, entityIdMetadataMap, manualProcess, recruiterId);
                if (Boolean.TRUE.equals(isSuccess)) {
                    log.info("Success publishing messages of {}, client {} to Bullhorn sns topic", entityName,
                            clientId);
                    return ValidationResult.builder()
                            .isValidJob(Boolean.TRUE)
                            .build();
                }
            }
        }

        log.info("Error publishing messages of {}, client {} to Bullhorn sns topic", entityName, clientId);
        return ValidationResult.builder()
                .isValidJob(Boolean.FALSE)
                .build();
    }

    private <T> Boolean processEvents(Map<String, List<String>> entityIdFieldsMap, String entityName,
                                      int clientId, T data, Map<Integer, String> entityIdEventTypeMap,
                                      Map<String, Map<String, List<Integer>>> entityIdMetadataMap, boolean manualProcess, int recruiterId) {
        log.info("Started publishing {} {} entities for client {} as events to bullhorn sns topic", entityName,
                entityIdEventTypeMap.keySet(), clientId);
        Map<Integer, String> dataMap = new HashMap<>();
        int entityId = -1;
        try {
            if (data instanceof List<?>) {
                for (Object o : (List<?>) data) {
                    LinkedHashMap<String, Object> linkedHashMap = (LinkedHashMap<String, Object>) o;
                    entityId = (int) linkedHashMap.get("id");
                    linkedHashMap.put("isDoProcessManual", manualProcess);
                    dataMap.put(entityId, objectMapper.writeValueAsString(linkedHashMap));
                }
            } else {
                LinkedHashMap<String, Object> linkedHashMap = (LinkedHashMap<String, Object>) data;
                entityId = (int) linkedHashMap.get("id");
                linkedHashMap.put("isDoProcessManual", manualProcess);
                dataMap.put(entityId, objectMapper.writeValueAsString(linkedHashMap));
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
                    .recruiterId(recruiterId)
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

    private final Map<String, PropertyDescriptor> propertyDescriptorCache = Arrays.stream(requiredFieldsConfig.split(","))
            .collect(Collectors.toMap(field -> field, field -> {
                try {
                    return new PropertyDescriptor(field, JobOrder.class);
                } catch (IntrospectionException e) {
                    log.error("Error initializing property descriptor for field: {}", field, e);
                    return null;
                }
            }));

    public ValidationResult validateAndBuildJob(JobOrder job, Map<String, String> incomingValues) {
        log.info("Started validating JobsDetail {} from Bullhorn for voice ai fields", job.getId());
        boolean isValid = true;
        Map<String, String> missingFields = new HashMap<>();

        for (Map.Entry<String, PropertyDescriptor> entry : propertyDescriptorCache.entrySet()) {
            String field = entry.getKey();
            PropertyDescriptor propertyDescriptor = entry.getValue();
            Method getter = propertyDescriptor.getReadMethod();
            Method setter = propertyDescriptor.getWriteMethod();

            try {
                if (getter != null) {
                    String modelValue = (String) getter.invoke(job);
                    String incomingValue = incomingValues != null ? incomingValues.get(field) : null;
                    if (CommonUtils.isNullOrEmpty(incomingValue)) {
                        if (CommonUtils.isNullOrEmpty(modelValue)) {
                            isValid = false;
                            missingFields.put(field, "");
                        } else {
                            missingFields.put(field, modelValue);
                        }
                    } else {
                        if (setter != null) {
                            setter.invoke(job, incomingValue);
                        }
                        missingFields.put(field, incomingValue);
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Error processing field: {}", field, e);
            }
        }

        return ValidationResult.builder()
                .isValidJob(isValid)
                .missingFields(missingFields)
                .build();
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
            ResponseEntity<GenericResponse<Object>> responseEntity = entityService.getMultipleEntities(
                    StringUtils.join(addedIds, ","), Constants.CANDIDATE, clientId, fields,
                    addedIds.size(), 0);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.hasBody()) {
                GenericResponse<Object> body = responseEntity.getBody();
                if (body != null && body.getData() != null) {
                    if (body.getData() instanceof List) {
                        candidates = objectMapper.convertValue(body.getData(),
                                new TypeReference<List<Candidate>>() {});
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
                .isDoProcessManual(baseTearsheet.getIsDoProcessManual())
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
