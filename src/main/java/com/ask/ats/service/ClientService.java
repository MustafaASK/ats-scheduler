package com.ask.ats.service;

import com.ask.ats.handler.GlobalExceptionHandler;
import com.ask.ats.model.BullhornResponse;
import com.ask.ats.model.EventResponse;
import com.ask.ats.model.GenericResponse;
import com.ask.ats.model.jobdiva.CandidateNotesListResponse;
import com.ask.ats.model.jobdiva.GetResponse;
import com.ask.ats.publisher.BullhornEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.ask.ats.utils.CommonUtils.buildCuratelyResponse;
import static com.ask.ats.utils.CommonUtils.buildGenericResponse;

/**
 * The type Client service.
 */
@Service
@Slf4j
public class ClientService {

    @Value("${atsService.api.url}")
    private String atsServiceApiUrl;


    private final RestTemplate restTemplate;
    private final GlobalExceptionHandler exceptionHandler;
    private final ObjectMapper objectMapper;
    private final BullhornEventPublisher bullhornEventPublisher;


    /**
     * Instantiates a new Client service.
     *
     * @param restTemplate           the rest template
     * @param exceptionHandler       the exception handler
     * @param objectMapper           the object mapper
     * @param bullhornEventPublisher the bullhorn event publisher
     */
    public ClientService(RestTemplate restTemplate, GlobalExceptionHandler exceptionHandler, ObjectMapper objectMapper, BullhornEventPublisher bullhornEventPublisher) {
        this.restTemplate = restTemplate;
        this.exceptionHandler = exceptionHandler;
        this.objectMapper = objectMapper;
        this.bullhornEventPublisher = bullhornEventPublisher;
    }


    /**
     * Gets multiple entities.
     *
     * @param <T>        the type parameter
     * @param entityIds  the entity ids
     * @param entityName the entity name
     * @param clientId   the client id
     * @param fields     the fields
     * @param count      the count
     * @param start      the start
     * @return the multiple entities
     */
    public <T> ResponseEntity<GenericResponse<T>> getMultipleEntities(
            String entityIds, String entityName, int clientId, String fields, int count, int start) {

        String apiUrl = "%s/bullhorn/%d/%s/%s?fields=%s&count=%d&start=0".formatted(atsServiceApiUrl, clientId,
                entityName, entityIds, fields, count);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<BullhornResponse<T>> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            String responseJson = objectMapper.writeValueAsString(response);
            log.info("Successfully fetched multiple {}s for client {} with IDs {}", entityName, clientId, entityIds);

            BullhornResponse<T> body = response.getBody();
            String message = body != null ? "Successfully fetched multiple %ss".formatted(entityName) : null;
            T data = body != null ? body.getData() : null;

            return buildGenericResponse(response.getStatusCode(), data, responseJson, message, Boolean.TRUE);

        } catch (RestClientException ex) {
            return exceptionHandler.handleRestClientException(ex, "Client Error while fetching multiple " + entityName);
        } catch (JsonProcessingException ex) {
            return exceptionHandler.handleJsonException(ex, "JSON Error while processing response for " + entityName);
        } catch (Exception e) {
            return exceptionHandler.handleGeneralException(e, "Unexpected error while fetching multiple " + entityName);
        }
    }

    /**
     * Gets jobs.
     *
     * @param <T>              the type parameter
     * @param clientId         the client id
     * @param jobDivaRecruiter the job diva recruiter
     * @param lastUpdated      the last updated
     * @return the jobs
     */
    public <T> ResponseEntity<GenericResponse<T>> getJobs(int clientId, int jobDivaRecruiter, String lastUpdated) {

        String url = "%s/agileOne/getAgileOneJobs/%d/%d?lastUpdated=%s&processEvents=%s".formatted(atsServiceApiUrl, clientId, jobDivaRecruiter, "2025-04-12", Boolean.FALSE);
        try {
            return restTemplate.exchange(url, HttpMethod.GET,
                    null, new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException ex) {
            return exceptionHandler.handleRestClientException(ex, "Client Error while fetching jobs from agileOne");
        } catch (Exception e) {
            return exceptionHandler.handleGeneralException(e, "Error while fetching jobs from agileOne");
        }
    }


    /**
     * Gets assignments.
     *
     * @param <T>              the type parameter
     * @param clientId         the client id
     * @param lastUpdated      the last updated
     * @param jobDivaRecruiter the job diva recruiter
     * @return the assignments
     */
    public <T> ResponseEntity<GenericResponse<List<T>>> getAssignments(int clientId, String lastUpdated, int jobDivaRecruiter) {

        String url = "%s/agileOne/getAgileOneAssignments/%d/%d?lastUpdated=%s&processEvents=%s".formatted(atsServiceApiUrl, clientId, jobDivaRecruiter, lastUpdated, Boolean.FALSE);

        try {
            return restTemplate.exchange(url, HttpMethod.GET,
                    null, new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException ex) {
            log.error("Client Error while fetching assignments from agileOne for clientId: {} and lastUpdated: {} - {}",
                    clientId, lastUpdated, ex.getMessage(), ex);
            return exceptionHandler.handleRestClientException(ex, "Client Error while fetching assignments from agileOne");
        } catch (Exception e) {
            log.error("Unexpected error while fetching assignments from agileOne for clientId: {} and lastUpdated: {} - {}",
                    clientId, lastUpdated, e.getMessage(), e);
            return exceptionHandler.handleGeneralException(e, "Error while fetching assignments from agileOne");
        }
    }

    /**
     * Gets details.
     *
     * @param <T>            the type parameter
     * @param entityIds      the entity ids
     * @param userFieldsName the user fields name
     * @param entityName     the entity name
     * @param clientId       the client id
     * @return the details
     */
    public <T> ResponseEntity<GenericResponse<List<T>>> getDetails(String entityIds, String userFieldsName, String entityName, int clientId) {

        String apiUrl = "%s/jobDiva/getDetails/%d?entityName=%s&ids=%s&userFieldsName=%s".formatted(atsServiceApiUrl, clientId, entityName, entityIds, userFieldsName);

        try {
            ResponseEntity<GetResponse<T>> response = restTemplate.exchange(apiUrl, HttpMethod.GET,
                    null, new ParameterizedTypeReference<>() {
                    });
            String responseJson = objectMapper.writeValueAsString(response);
            log.info("Successfully fetched {} from job diva with status {}", entityName, response.getStatusCode());

            GetResponse<T> body = response.hasBody() ? response.getBody() : null;
            String message = "Successfully fetched %s".formatted(entityName);
            List<T> data = null;

            if (body != null) {
                data = body.getData();
            }
            return buildCuratelyResponse(HttpStatus.OK, data, responseJson, message, Boolean.TRUE);
        } catch (RestClientException ex) {
            return exceptionHandler.handleRestClientException(ex, "Client Error while fetching job diva %s"
                    .formatted(entityName));
        } catch (JsonProcessingException ex) {
            return exceptionHandler.handleJsonException(ex, "Json Error while fetching job diva %s"
                    .formatted(entityName));
        } catch (Exception e) {
            return exceptionHandler.handleGeneralException(e, "Error while fetching job diva %s".formatted(entityName));
        }
    }

    /**
     * Job agent search response entity.
     *
     * @param <T>         the type parameter
     * @param clientId    the client id
     * @param jobId       the job id
     * @param resumeCount the resume count
     * @return the response entity
     */
    public <T> ResponseEntity<GenericResponse<List<T>>> jobAgentSearch(int clientId, String jobId, int resumeCount) {
        String apiUrl = "%s/jobDiva/jobAgentSearch/%d?jobId=%s&resumeCount=%d".formatted(atsServiceApiUrl, clientId, jobId, resumeCount);
        try {
            ResponseEntity<GenericResponse<List<T>>> response = restTemplate.exchange(apiUrl, HttpMethod.GET,
                    null, new ParameterizedTypeReference<>() {
                    });
            String responseJson = objectMapper.writeValueAsString(response);
            log.info("Successful job agent search in job diva with status {}", response.getStatusCode());

            List<T> body = response.hasBody() ? Objects.requireNonNull(response.getBody()).getData() : Collections.emptyList();
            String message = "Successful job agent search in job diva";
            return buildGenericResponse(HttpStatus.OK, body, responseJson, message, true);
        } catch (RestClientException ex) {
            return exceptionHandler.handleRestClientException(ex, "Client Error while job agent search in job diva");
        } catch (JsonProcessingException ex) {
            return exceptionHandler.handleJsonException(ex, "Json Error while job agent search in job diva");
        } catch (Exception e) {
            return exceptionHandler.handleGeneralException(e, "Error while job agent search in job diva");
        }
    }

    /**
     * Gets candidate notes list.
     *
     * @param clientId     the client id
     * @param candidateIds the candidate ids
     * @return the candidate notes list
     */
    public ResponseEntity<GenericResponse<CandidateNotesListResponse>> getCandidateNotesList(int clientId, String candidateIds) {
        String apiUrl = "%s/jobDiva/getCandidateNotesList?clientId=%s&candidateIds=%s".formatted(atsServiceApiUrl, clientId, candidateIds);

        try {
            ResponseEntity<GenericResponse<CandidateNotesListResponse>> response = restTemplate.exchange(
                    apiUrl.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });

            String responseJson = objectMapper.writeValueAsString(response);
            log.info("Successfully fetched candidate notes list from job diva with status {}", response.getStatusCode());

            CandidateNotesListResponse body = response.hasBody() ? Objects.requireNonNull(response.getBody()).getData() : null;
            String message = "Successfully fetched candidate notes list";
            return buildGenericResponse(HttpStatus.OK, body, responseJson, message, true);
        } catch (RestClientException ex) {
            return exceptionHandler.handleRestClientException(ex, "Client Error while fetching candidate notes list");
        } catch (JsonProcessingException ex) {
            return exceptionHandler.handleJsonException(ex, "Json Error while fetching candidate notes list");
        } catch (Exception e) {
            return exceptionHandler.handleGeneralException(e, "Error while fetching candidate notes list");
        }
    }

    /**
     * Gets new updated candidate notes.
     *
     * @param <T>        the type parameter
     * @param clientId   the client id
     * @param fromDate   the from date
     * @param toDate     the to date
     * @param pageNumber the page number
     * @return the new updated candidate notes
     */
    public <T> ResponseEntity<GenericResponse<List<T>>> getNewUpdatedCandidateNotes(int clientId, String fromDate, String toDate, int pageNumber) {
        String apiUrl = "%s/jobDiva/getNewUpdatedCandidateNotes/%d?fromDate=%s&toDate=%s&pageNumber=%s".formatted(atsServiceApiUrl, clientId, fromDate, toDate, pageNumber);
        try {
            ResponseEntity<GetResponse<T>> response = restTemplate.exchange(apiUrl, HttpMethod.GET,
                    null, new ParameterizedTypeReference<>() {
                    });
            String responseJson = objectMapper.writeValueAsString(response);
            log.info("Successful get new updated candidate notes from job diva for page {}, client {}: {}", pageNumber, clientId,
                    response.getStatusCode());

            GetResponse<T> body = response.hasBody() ? response.getBody() : null;
            List<T> data = Collections.emptyList();
            if (body != null) {
                data = body.getData();
            }
            String message = "Successful get new updated candidate notes from job diva";
            return buildGenericResponse(HttpStatus.OK, data, responseJson, message, true);
        } catch (RestClientException ex) {
            return exceptionHandler.handleRestClientException(ex,
                    "Client Error while getting new updated candidate notes from job diva");
        } catch (JsonProcessingException ex) {
            return exceptionHandler.handleJsonException(ex, "Json Error while getting new updated candidate notes from job diva");
        } catch (Exception e) {
            return exceptionHandler.handleGeneralException(e, "Error while getting new updated candidate notes from job diva");
        }
    }


    /**
     * Fetch list of events response entity.
     *
     * @param clientId    the client id
     * @param recruiterId the recruiter id
     * @return the response entity
     */
    public ResponseEntity<GenericResponse<EventResponse>> fetchListOfEvents(int clientId, int recruiterId) {
        try {
            String apiUrl = "%s/bullhorn/getListOfEvents/%d?recruiterId=%s".formatted(atsServiceApiUrl, clientId, recruiterId);

            ResponseEntity<EventResponse> response = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, null, new ParameterizedTypeReference<EventResponse>() {
                    });

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && response.getBody().getEvents() != null
                    && !response.getBody().getEvents().isEmpty()) {

                EventResponse eventResponse = response.getBody();

                List<EventResponse.Event> events = eventResponse.getEvents();

                if (!events.isEmpty()) {
                    bullhornEventPublisher.processEventsAsync(events, clientId);
                }

                return buildCuratelyResponse(response.getStatusCode(), eventResponse,
                        objectMapper.writeValueAsString(response), "Successfully fetched the latest events",
                        Boolean.TRUE);
            } else {
                log.info("No new events found: Subscription ID = {}", response.getBody());
                return buildCuratelyResponse(response.getStatusCode(), response.getBody(),
                        objectMapper.writeValueAsString(response),
                        "No new events available for the given subscription and request ID", Boolean.TRUE);
            }
        } catch (RestClientException ex) {
            return exceptionHandler.handleRestClientException(ex, "Error while fetching list of events");
        } catch (JsonProcessingException ex) {
            return exceptionHandler.handleJsonException(ex, "Json Error while fetching list of events");
        } catch (Exception e) {
            return exceptionHandler.handleGeneralException(e, "Unexpected error while fetching list of events");
        }
    }
}
