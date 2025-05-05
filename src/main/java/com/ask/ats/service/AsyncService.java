package com.ask.ats.service;

import com.ask.ats.model.Applicant;
import com.ask.ats.model.EventResponse;
import com.ask.ats.model.GenericResponse;
import com.ask.ats.model.agileone.AssignmentDetails;
import com.ask.ats.model.agileone.JobDetails;
import com.ask.ats.model.jobdiva.*;
import com.ask.ats.publisher.AgileOnePublisher;
import com.ask.ats.publisher.JobDivaPublisher;
import com.ask.ats.repository.CuratelyRepository;
import com.ask.ats.utils.CommonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishBatchRequest;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;
import software.amazon.awssdk.services.sns.model.PublishBatchResponse;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.ask.ats.utils.Constants.*;

/**
 * The type Async service.
 */
@Service
@Slf4j
public class AsyncService {

    @Value("${aws.sns.jobDivaTopicArn}")
    private String snsTopicArn;


    private final Executor executor;
    private final JobDivaPublisher jobDivaPublisher;
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final ClientService clientService;
    private final AgileOnePublisher agileOnePublisher;
    private final CuratelyRepository curatelyRepository;
    /**
     * The Formatter.
     */
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    /**
     * Instantiates a new Async service.
     *
     * @param executor           the executor
     * @param jobDivaPublisher   the job diva publisher
     * @param snsClient          the sns client
     * @param objectMapper       the object mapper
     * @param clientService      the client service
     * @param agileOnePublisher  the agile one publisher
     * @param curatelyRepository the curately repository
     */
    public AsyncService( @Qualifier("taskExecutor") Executor executor, JobDivaPublisher jobDivaPublisher, SnsClient snsClient, ObjectMapper objectMapper, ClientService clientService, AgileOnePublisher agileOnePublisher, CuratelyRepository curatelyRepository) {
        this.executor = executor;
        this.jobDivaPublisher = jobDivaPublisher;
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.clientService = clientService;
        this.agileOnePublisher = agileOnePublisher;
        this.curatelyRepository = curatelyRepository;
    }


    private void publishBatch(List<PublishBatchRequestEntry> publishBatchRequestEntries) throws RuntimeException {
        PublishBatchRequest publishRequest = PublishBatchRequest.builder()
                .topicArn(snsTopicArn)
                .publishBatchRequestEntries(publishBatchRequestEntries)
                .build();
        PublishBatchResponse publishResponse = snsClient.publishBatch(publishRequest);
        log.info("Published message successfully: {}", publishResponse.successful());
    }


    /**
     * Process new candidate notes.
     *
     * @param clientId    the client id
     * @param recruiterId the recruiter id
     * @throws DataAccessException the data access exception
     */
    public void processNewCandidateNotes(int clientId, int recruiterId) throws DataAccessException {
        Timestamp fromDate = jobDivaPublisher.fetchFromDate(clientId);
        Date toDate = new Date();
        Map<String, List<String>> jobCandidateIdsMap = jobDivaPublisher.fetchUpdatedCandidateNotes(clientId, fromDate, toDate);
        if (jobCandidateIdsMap == null || jobCandidateIdsMap.isEmpty()) {
            log.info("No new candidate notes to process for client {}, recruiter {}", clientId, recruiterId);
            return;
        }
        List<String> atsValues = jobCandidateIdsMap.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(candidateId -> entry.getKey() + "-" + candidateId))
                .toList();
        List<Applicant> voiceAiSourcedApplicants = curatelyRepository.filterVoiceAiApplicants(clientId, atsValues);
        Map<String, List<Applicant>> jobSourcedApplicantsMap = voiceAiSourcedApplicants.stream()
                .collect(Collectors.groupingBy(applicant -> applicant.getJobId() + "-applicants"));

        List<Applicant> voiceAiOpenResumes = curatelyRepository.filterVoiceAiOpenResumes(clientId, atsValues);
        jobSourcedApplicantsMap.putAll(voiceAiOpenResumes.stream()
                .collect(Collectors.groupingBy(applicant -> applicant.getJobId() + "-shortList")));

        if (jobSourcedApplicantsMap != null && !jobSourcedApplicantsMap.isEmpty()) {
            List<PublishBatchRequestEntry> publishRequests = new ArrayList<>();
            List<CompletableFuture<Void>> tasks = jobSourcedApplicantsMap.entrySet().stream()
                    .map(entry -> CompletableFuture.runAsync(() -> {
                        try {
                            log.info("Starting async processCandidateNotes for JobDiva job id {}", entry.getKey());
                            String[] splitKey = entry.getKey().split("-");
                            String jobId = splitKey[0];
                            String entityName = splitKey[1];
                            PublishBatchRequestEntry requestEntry = jobDivaPublisher.processCandidateNotes(clientId, recruiterId,
                                    entry.getValue(), entityName, jobId);
                            if (requestEntry != null) {
                                publishRequests.add(requestEntry);
                            }
                            log.info("Finished async processCandidateNotes for JobDiva job id {}", entry.getKey());
                        } catch (Exception e) {
                            log.error("Error processing async candidate notes of job diva {}, recruiter {}, client {}: {}", entry.getKey(),
                                    recruiterId, clientId, e.getMessage(), e);
                        }
                    }, executor))
                    .toList();

            CompletableFuture<Void> allTasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                    .exceptionally(ex -> {
                        log.error("Error completing async tasks: {}", ex.getMessage(), ex);
                        return null;
                    });

            allTasks.thenRun(() -> {
                try {
                    if (!publishRequests.isEmpty()) {
                        int size = publishRequests.size();
                        for (int i = 0; i < size; i += 10) {
                            int end = Math.min(i + 10, size);
                            publishBatch(publishRequests.subList(i, end));
                        }
                        log.info("Published {} job diva events of JobsDetail, client {} to Ats sns topic",
                                size, clientId);
                    }
                    jobDivaPublisher.saveJobsEvent(clientId, jobSourcedApplicantsMap.size(), fromDate, toDate);
                } catch (RuntimeException e) {
                    log.error("Error publishing {} job diva events of JobsDetail, client {} to Ats sns topic",
                            publishRequests.size(), clientId, e);
                }
            });
        } else {
            log.warn("Error fetching jobs data or empty response from job diva for client {}, recruiter {}", clientId,
                    recruiterId);
        }
    }

    /**
     * Process job applications async.
     *
     * @param <T>         the type parameter
     * @param clientId    the client id
     * @param recruiterId the recruiter id
     */
    public <T> void processJobApplicationsAsync(int clientId, int recruiterId) {

        Map<String, Set<JobApplicant>> groupedApplicants = jobDivaPublisher.scheduleJobApplications(clientId);
        if (groupedApplicants.isEmpty()) {
            log.info("No job applications to process for client {}", clientId);
            return;
        }

        List<CompletableFuture<Void>> tasks;
        List<PublishBatchRequestEntry> publishRequests = new ArrayList<>();
        List<String> jobIds = new ArrayList<>(groupedApplicants.keySet());

        tasks = jobIds.stream()
                .map(jobId -> CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Starting async processJob for Scheduled JobDiva job id {}", jobId);
                        Job job = Job.builder()
                                .id(jobId)
                                .build();
                        PublishBatchRequestEntry requestEntry = jobDivaPublisher.processJob(
                                clientId, recruiterId, job, groupedApplicants, Boolean.FALSE);
                        if (requestEntry != null) {
                            publishRequests.add(requestEntry);
                        }
                        log.info("Finished Scheduled processJob for JobDiva job id {}", jobId);
                    } catch (Exception e) {
                        log.error("Error processing Async {} of {} {}, recruiter {}, client {}: {}",
                                JOB_DIVA, JOB_DETAIL, jobId, recruiterId, clientId, e.getMessage(), e);
                    }
                }, executor))
                .toList();

        CompletableFuture<Void> allTasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .exceptionally(ex -> {
                    log.error("Error completing async tasks: {}", ex.getMessage(), ex);
                    return null;
                });

        allTasks.thenRun(() -> {
            try {
                if (!publishRequests.isEmpty()) {
                    int size = publishRequests.size();
                    for (int i = 0; i < size; i += 10) {
                        int end = Math.min(i + 10, size);
                        publishBatch(publishRequests.subList(i, end));
                    }
                    log.info("Published {} {} events of {}, client {} to Ats sns topic for Scheduled", size,
                            JOB_DIVA, JOB_DETAIL, clientId);
                }
            } catch (RuntimeException e) {
                log.error("Error while publishing {} {} events of {}, client {} to Ats sns topic", publishRequests.size(),
                        JOB_DIVA, JOB_DETAIL, clientId, e);
            }
        });
    }

    /**
     * Process agile one assignments response entity.
     *
     * @param <T>             the type parameter
     * @param clientId        the client id
     * @param date            the date
     * @param recruiterId     the recruiter id
     * @param isProcessEvents the is process events
     * @return the response entity
     */
    public <T> ResponseEntity<GenericResponse<List<T>>> processAgileOneAssignments(
            int clientId, String date, int recruiterId, Boolean isProcessEvents) {

        ResponseEntity<GenericResponse<List<T>>> response = agileOnePublisher.getAssignmentList(clientId, date, recruiterId);
        GenericResponse<List<T>> body = response.getBody();

        if (CommonUtils.isInvalidResponse(body)) {
            log.warn("Invalid response from get agile one assignments api: {}", response);
            return ResponseEntity.ok().body(body);
        }

        if (body.getData().isEmpty()) {
            log.info("No Assignments to process for the updated date: {}", date);
            return ResponseEntity.ok().body(body);
        }

        List<AssignmentDetails> assignments = (List<AssignmentDetails>) convertResponse(body, AssignmentDetails.class);
        if (Boolean.FALSE.equals(isProcessEvents)) {
            return response;
        }

        LocalDateTime storedLastActivityDate = curatelyRepository.getLatestLastActivityDate(clientId,
                "AgileOneAssignment");

        List<AssignmentDetails> filteredAssignments = (storedLastActivityDate == null)
                ? assignments
                : assignments.stream()
                .filter(job -> {
                    ZonedDateTime assignmentDate = ZonedDateTime.parse(job.getLastActivityDate(), formatter);
                    ZonedDateTime localDateTime = assignmentDate.withZoneSameInstant(ZoneId.of("America/New_York"));
                    return localDateTime.toLocalDateTime().isAfter(storedLastActivityDate);
                })
                .sorted(Comparator.comparing(AssignmentDetails::getLastActivityDate))
                .toList();

        if (filteredAssignments.isEmpty()) {
            log.info("No new assignments to publish for client {} after lastActivityDate {}", clientId, storedLastActivityDate);
            response.getBody().setMessage("No new assignments to publish after lastActivityDate %s".formatted(storedLastActivityDate));
            return response;
        }
        processAssignmentsAsync(clientId, recruiterId, filteredAssignments);
        return response;
    }

    /**
     * Process assignments async.
     *
     * @param clientId            the client id
     * @param recruiterId         the recruiter id
     * @param filteredAssignments the filtered assignments
     */
    public void processAssignmentsAsync(int clientId, int recruiterId, List<AssignmentDetails> filteredAssignments) {
        log.info("Started Processing Assignments asynchronously Based on the LastActivityDate");
        List<String> atsValues = agileOnePublisher.getAssignmentAtsValue(clientId);
        List<PublishBatchRequestEntry> publishRequests = new ArrayList<>();
        List<CompletableFuture<Void>> tasks = filteredAssignments.stream()
                .filter(assignmentDetails -> atsValues.contains(String.valueOf(assignmentDetails.getAssignmentID())))
                .map(assignmentDetails -> CompletableFuture.runAsync(() -> {
                    try {

                        PublishBatchRequestEntry requestEntry = agileOnePublisher.processAssignmentFetch(clientId, assignmentDetails, recruiterId);
                        if (requestEntry != null) {
                            publishRequests.add(requestEntry);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, executor))
                .toList();

        CompletableFuture<Void> allTasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .exceptionally(ex -> {
                    log.error("Error completing async tasks: {}", ex.getMessage(), ex);
                    return null;
                });

        allTasks.thenRun(() -> {
            try {
                if (!publishRequests.isEmpty()) {
                    int size = publishRequests.size();
                    for (int i = 0; i < size; i += 10) {
                        int end = Math.min(i + 10, size);
                        agileOnePublisher.publishBatch(publishRequests.subList(i, end));
                    }
                    log.info("Published {} Assignment events for client {}", size, clientId);
                }
                LocalDateTime latestLastActivityDate = filteredAssignments.stream()
                        .map(AssignmentDetails::getLastActivityDate)
                        .map(dateStr -> {
                            ZonedDateTime assignmentDate = ZonedDateTime.parse(dateStr, formatter);
                            ZonedDateTime localDateTime = assignmentDate.withZoneSameInstant(ZoneId.of("America/New_York"));
                            return localDateTime.toLocalDateTime();
                        })
                        .max(LocalDateTime::compareTo)
                        .orElse(null);

                if (latestLastActivityDate != null) {
                    curatelyRepository.saveLastActivityDate(clientId, "AgileOneAssignment", latestLastActivityDate,
                            filteredAssignments.size());
                    log.info("Updated lastActivityDateMapAssignment for client {} to {}", clientId, latestLastActivityDate);
                }
            } catch (RuntimeException e) {
                log.error("Error publishing assignment events for client {}", clientId, e);
            }
        });
    }

    /**
     * Process agile one jobs response entity.
     *
     * @param <T>             the type parameter
     * @param clientId        the client id
     * @param recruiterId     the recruiter id
     * @param date            the date
     * @param isProcessEvents the is process events
     * @return the response entity
     */
    public <T> ResponseEntity<GenericResponse<List<T>>> processAgileOneJobs(int clientId, int recruiterId,
                                                                                 String date, Boolean isProcessEvents) {

        ResponseEntity<GenericResponse<List<T>>> response = agileOnePublisher.getJobsFromAgile(clientId, date, recruiterId);
        GenericResponse<List<T>> body = response.getBody();
        if (CommonUtils.isInvalidResponse(body)) {
            log.warn("Invalid response from get agile one jobs API: {}", response);
            return ResponseEntity.ok().body(body);
        }

        if (body.getData().isEmpty()) {
            log.info("No jobs to process for the updated date: {}", date);
            return ResponseEntity.ok().body(body);
        }

        List<JobDetails> jobs = (List<JobDetails>) convertResponse(body, JobDetails.class);
        if (Boolean.FALSE.equals(isProcessEvents)) {
            return response;
        }

        LocalDateTime storedLastActivityDate = curatelyRepository.getLatestLastActivityDate(clientId,
                "AgileOneJob");
        List<JobDetails> filteredJobs = (storedLastActivityDate == null)
                ? jobs
                : jobs.stream()
                .filter(job -> {
                    ZonedDateTime jobDate = ZonedDateTime.parse(job.getLastActivityDate(), formatter);
                    ZonedDateTime localDateTime = jobDate.withZoneSameInstant(ZoneId.of("America/New_York"));
                    return localDateTime.toLocalDateTime().isAfter(storedLastActivityDate);
                })
                .sorted(Comparator.comparing(JobDetails::getLastActivityDate))
                .toList();

        if (filteredJobs.isEmpty()) {
            log.info("No new jobs to publish for client {} after lastActivityDate {}", clientId, storedLastActivityDate);
            response.getBody().setMessage("No new jobs to publish after lastActivityDate %s".formatted(storedLastActivityDate));
            return response;
        }

        processJobsAsync(clientId, recruiterId, filteredJobs);
        return response;
    }


    /**
     * Process jobs async.
     *
     * @param clientId     the client id
     * @param recruiterId  the recruiter id
     * @param filteredJobs the filtered jobs
     */
    public void processJobsAsync(int clientId, int recruiterId, List<JobDetails> filteredJobs) {
        log.info("Started Processing Jobs asynchronously Based on the LastActivityDate");
        List<String> atsValues = agileOnePublisher.getJobAtsValues(clientId);
        List<PublishBatchRequestEntry> publishRequests = new ArrayList<>();
        List<CompletableFuture<Void>> tasks = filteredJobs.stream()
                .map(job -> CompletableFuture.runAsync(() -> {
                    try {
                        String eventType = atsValues.contains(String.valueOf(job.getJobId())) ? "Update" : "Insert";
                        PublishBatchRequestEntry requestEntry = agileOnePublisher.processJobFetch(clientId, job,
                                recruiterId, eventType);
                        if (requestEntry != null) {
                            publishRequests.add(requestEntry);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, executor))
                .toList();

        CompletableFuture<Void> allTasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .exceptionally(ex -> {
                    log.error("Error completing async tasks: {}", ex.getMessage(), ex);
                    return null;
                });

        allTasks.thenRun(() -> {
            try {
                if (!publishRequests.isEmpty()) {
                    int size = publishRequests.size();
                    for (int i = 0; i < size; i += 10) {
                        int end = Math.min(i + 10, size);
                        agileOnePublisher.publishBatch(publishRequests.subList(i, end));
                    }
                    log.info("Published {} job events for client {}", size, clientId);
                }
                LocalDateTime latestLastActivityDate = filteredJobs.stream()
                        .map(JobDetails::getLastActivityDate)
                        .map(dateStr -> {
                            ZonedDateTime jobDate = ZonedDateTime.parse(dateStr, formatter);
                            ZonedDateTime localDateTime = jobDate.withZoneSameInstant(ZoneId.of("America/New_York"));
                            return localDateTime.toLocalDateTime();
                        })
                        .max(LocalDateTime::compareTo)
                        .orElse(null);

                if (latestLastActivityDate != null) {
                    curatelyRepository.saveLastActivityDate(clientId, "AgileOneJob", latestLastActivityDate,
                            filteredJobs.size());
                    log.info("Updated lastActivityDateMap for client {} to {}", clientId, latestLastActivityDate);
                }
            } catch (RuntimeException e) {
                log.error("Error publishing job events for client {}", clientId, e);
            }
        });
    }

    /**
     * Convert response list.
     *
     * @param <T>        the type parameter
     * @param body       the body
     * @param modelClass the model class
     * @return the list
     */
    public <T> List<T> convertResponse(GenericResponse<List<T>> body, Class<?> modelClass) {
        return objectMapper.convertValue(
                Objects.requireNonNull(body).getData(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, modelClass)
        );
    }

    /**
     * Process jobs agent search.
     *
     * @param clientId    the client id
     * @param recruiterId the recruiter id
     */
    public void processJobsAgentSearch(int clientId, int recruiterId) {

        List<String> jobIds = jobDivaPublisher.getExistingJobAtsValues(clientId);
        if (jobIds.isEmpty()) {
            log.info("No jobs exist to process job agent search for client {}", clientId);
            return;
        }

        List<CompletableFuture<Void>> tasks;
        List<PublishBatchRequestEntry> publishRequests = new ArrayList<>();
        tasks = jobIds.stream()
                .map(jobId -> CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Starting async process agent search for job id {}", jobId);
                        List<Candidate> candidates = jobDivaPublisher.getNewJobAgentSearch(clientId,
                                jobId);

                        if (candidates.isEmpty()) {
                            return;
                        }
                        Set<String> candidateIds = candidates.stream()
                                .map(Candidate::getCandidateid)
                                .collect(Collectors.toSet());
                        List<String> filteredCandidates = jobDivaPublisher.filterCandidates(candidateIds, jobId,
                                clientId);

                        if (filteredCandidates.isEmpty()) {
                            return;
                        }
                        candidates = candidates.stream()
                                .filter(candidate -> filteredCandidates.contains(candidate.getCandidateid()))
                                .toList();

                        Job job = Job.builder()
                                .id(jobId)
                                .sourcedCandidates(candidates)
                                .build();
                        PublishBatchRequestEntry requestEntry = jobDivaPublisher.processJobUpdate(clientId, recruiterId,
                                job);
                        if (requestEntry != null) {
                            publishRequests.add(requestEntry);
                        }
                        log.info("Finished async process agent search for job id {}", jobId);
                    } catch (Exception e) {
                        log.error("Error processing async {} of job agent search {}, recruiter {}, client {}: {}",
                                JOB_DIVA, jobId, recruiterId, clientId, e.getMessage(), e);
                    }
                }, executor))
                .toList();

        CompletableFuture<Void> allTasks = CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .exceptionally(ex -> {
                    log.error("Error completing async tasks: {}", ex.getMessage(), ex);
                    return null;
                });

        allTasks.thenRun(() -> {
            try {
                if (!publishRequests.isEmpty()) {
                    int size = publishRequests.size();
                    for (int i = 0; i < size; i += 10) {
                        int end = Math.min(i + 10, size);
                        publishBatch(publishRequests.subList(i, end));
                    }
                    log.info("Published {} {} events of job agent search, client {} to Ats sns topic for Scheduled",
                            size, JOB_DIVA, clientId);
                }
            } catch (RuntimeException e) {
                log.error("Error while publishing {} {} events of job agent search, client {} to Ats sns topic",
                        publishRequests.size(), JOB_DIVA, clientId, e);
            }
        });
    }

}

