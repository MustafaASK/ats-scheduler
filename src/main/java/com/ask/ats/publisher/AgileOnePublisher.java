package com.ask.ats.publisher;

import com.ask.ats.model.GenericResponse;
import com.ask.ats.model.SnsEvent;
import com.ask.ats.model.agileone.AssignmentDetails;
import com.ask.ats.model.agileone.JobDetails;
import com.ask.ats.repository.CuratelyRepository;
import com.ask.ats.service.ClientService;
import com.ask.ats.utils.Constants;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishBatchRequest;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;
import software.amazon.awssdk.services.sns.model.PublishBatchResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AgileOnePublisher {

    private final ClientService service;
    private final ObjectMapper objectMapper;
    private final SnsClient snsClient;
    private final CuratelyRepository curatelyRepository;

    @Value("${aws.sns.agileOneTopicArn}")
    private String snsTopicArn;

    public AgileOnePublisher(ClientService service, ObjectMapper objectMapper, SnsClient snsClient,
                             CuratelyRepository curatelyRepository) {
        this.service = service;

        this.objectMapper = objectMapper;
        this.snsClient = snsClient;
        this.curatelyRepository = curatelyRepository;
    }

    public <T> ResponseEntity<GenericResponse<List<T>>> getJobsFromAgile(int clientId, String date, int recruiterId) {
        return service.getJobs(clientId, recruiterId, date);
    }

    public List<String> getJobAtsValues(int clientId) {
        List<String> atsValues = curatelyRepository.getJobAtsValuesForAgileOne(clientId, Constants.AGILE);
        return atsValues;
    }

    public <T> ResponseEntity<GenericResponse<List<T>>> getAssignmentList(int clientId, String date, int recruiterId) {
        return service.getAssignments(clientId, date, recruiterId);
    }


    public List<String> getAssignmentAtsValue(int clientId) {
        List<String> atsValues = curatelyRepository.getAssignmentAtsValuesForAgileOne(clientId, Constants.AGILE);
        return atsValues;
    }

    private String convertModelToString(Object snsEvent) {
        try {
            return objectMapper.writeValueAsString(snsEvent);
        } catch (JsonProcessingException e) {
            log.error("Error converting sns event model to json string", e);
        }
        return null;
    }

    public PublishBatchRequestEntry processJobFetch(int clientId, JobDetails job, int recruiterId, String eventType) throws IOException {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        String dataString = convertModelToString(job);

        SnsEvent snsEvent = buildSnsEvent("Job", String.valueOf(job.getJobId()), clientId, dataString,
                eventType, recruiterId);
        String message = convertModelToString(snsEvent);
        if (message != null) {
            return PublishBatchRequestEntry.builder()
                    .message(message)
                    .id(UUID.randomUUID().toString())
                    .build();
        }
        log.error("Sns message is null for Job Detail {} from AgileOne for client {}", job.getJobId(), clientId);
        return null;
    }

    public PublishBatchRequestEntry processAssignmentFetch(int clientId, AssignmentDetails assignmentDetails, int recruiterId) throws IOException {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        String dataString = convertModelToString(assignmentDetails);

        SnsEvent snsEvent = buildSnsEvent("Assignment", String.valueOf(assignmentDetails.getAssignmentID()),
                clientId, dataString, "Update", recruiterId);
        String message = convertModelToString(snsEvent);
        if (message != null) {
            return PublishBatchRequestEntry.builder()
                    .message(message)
                    .id(UUID.randomUUID().toString())
                    .build();
        }
        log.error("Sns message is null for Assignment {} from AgileOne for client {}", assignmentDetails.getAssignmentID(), clientId);
        return null;
    }

    private SnsEvent buildSnsEvent(String entityName, String entityId, int clientId, String dataString, String eventType, int recruiterId) {
        return SnsEvent.builder()
                .entityName(entityName)
                .entityId(entityId)
                .eventType(eventType)
                .clientId(clientId)
                .data(dataString)
                .updatedFields(null)
                .metadata(null)
                .atsName(Constants.AGILE)
                .recruiterId(recruiterId)
                .build();
    }

    public void publishBatch(List<PublishBatchRequestEntry> publishBatchRequestEntries) throws RuntimeException {
        PublishBatchRequest publishRequest = PublishBatchRequest.builder()
                .topicArn(snsTopicArn)
                .publishBatchRequestEntries(publishBatchRequestEntries)
                .build();
        PublishBatchResponse publishResponse = snsClient.publishBatch(publishRequest);
        log.info("Published message successfully: {}", publishResponse.successful());
    }
}
