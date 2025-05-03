package com.ask.ats.publisher;

import com.ask.ats.model.Address;
import com.ask.ats.model.Applicant;
import com.ask.ats.model.GenericResponse;
import com.ask.ats.model.SnsEvent;
import com.ask.ats.model.jobdiva.*;
import com.ask.ats.repository.CuratelyRepository;
import com.ask.ats.service.ClientService;
import com.ask.ats.utils.CommonUtils;
import com.ask.ats.utils.CompressionUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.ask.ats.utils.Constants.*;

/**
 * The type Job diva publisher.
 */
@Slf4j
@Service
public class JobDivaPublisher {

    @Value("${jobDiva.resumeCount}")
    private int resumeCount;

    private static final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>(
            Map.of("atsName", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(JOB_DIVA)
                    .build()));

    private final ClientService clientService;
    private final ObjectMapper objectMapper;
    private final CuratelyRepository curatelyRepository;


    public JobDivaPublisher(ClientService clientService, ObjectMapper objectMapper,
                            CuratelyRepository curatelyRepository) {
        this.clientService = clientService;
        this.objectMapper = objectMapper;
        this.curatelyRepository = curatelyRepository;
    }


    public <T> PublishBatchRequestEntry processJob(int clientId, int recruiterId, Job job,
                                                   Map<String, Set<JobApplicant>> groupedApplicants,
                                                   boolean isSourcedEnabled) throws IOException {

        String jobId = job.getId();
        Set<String> candidateIds = new HashSet<>();
        List<String> sourcedIds = new ArrayList<>();
        if (isSourcedEnabled) {
            sourcedIds = getSourcedIds(clientId, jobId);
            candidateIds = new HashSet<>(sourcedIds);
        }

        Set<JobApplicant> jobApplicants = groupedApplicants.getOrDefault(jobId, Collections.emptySet());
        candidateIds.addAll(jobApplicants.stream().map(JobApplicant::getCandidateId)
                .collect(Collectors.toSet()));

        ResponseEntity<GenericResponse<List<T>>> response = fetchDetails(String.join(",", candidateIds), CANDIDATE_DETAIL, clientId);
        GenericResponse<List<T>> body = response.getBody();

        if (CommonUtils.isInvalidResponse(body)) {
            CommonUtils.handleErrorResponse(CANDIDATE_DETAIL, clientId, response);
            return null;
        }

        final List<Candidate> candidates = (List<Candidate>) convertResponse(body, Candidate.class);
        final Map<String, Candidate> candidateMap = candidates.stream().collect(Collectors.toMap(
                Candidate::getCandidateid,
                candidate -> candidate
        ));

        Set<String> candidateIdsSet = new HashSet<>(candidateIds);
        candidateIds = filterCandidates(candidateIdsSet, jobId, clientId).stream().collect(Collectors.toSet());
        final Map<String, Candidate> filteredCandidateMap = candidateIds.stream()
                .filter(candidateMap::containsKey)
                .collect(Collectors.toMap(
                        candidateId -> candidateId,
                        candidateId -> candidateMap.get(candidateId)
                ));
        jobApplicants = jobApplicants.stream()
            .filter(jobApplicant -> filteredCandidateMap.containsKey(jobApplicant.getCandidateId()))
            .map(jobApplicant -> {
                String candidateId = jobApplicant.getCandidateId();
                Candidate candidate = filteredCandidateMap.get(candidateId);
                jobApplicant.setCellPhone(candidate.getCellPhone());
                Address address = Address.builder()
                    .address1(candidate.getAddress1())
                    .address2(candidate.getAddress2())
                    .city(candidate.getCity())
                    .state(candidate.getState())
                    .zip(candidate.getZipCode())
                    .countryName(candidate.getCountry())
                    .build();
                jobApplicant.setAddress(address);
                return jobApplicant;
            })
            .collect(Collectors.toSet());

        job.setApplicants(jobApplicants.stream().toList());

        if (isSourcedEnabled) {
            List<Candidate> sourcedCandidates = sourcedIds.stream()
                    .filter(filteredCandidateMap::containsKey)
                    .map(id -> filteredCandidateMap.get(id))
                    .filter(Objects::nonNull)
                    .toList();

            enrichCandidatesWithResume(sourcedCandidates, new HashSet<>(sourcedIds), clientId);
            job.setSourcedCandidates(sourcedCandidates);
        } else {
            job.setSourcedCandidates(Collections.emptyList());
        }
        job.setDoProcessManual(Boolean.TRUE);

        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        String dataString = convertModelToString(job);

        SnsEvent snsEvent = buildSnsEvent("Job", jobId, clientId, dataString, recruiterId,
                isSourcedEnabled ? "Insert" : "Update");
        String message = convertModelToString(snsEvent);

        if (message != null) {
            String base64EncodedString = CompressionUtil.compress(message);
            return PublishBatchRequestEntry.builder()
                    .message(base64EncodedString)
                    .id(UUID.randomUUID().toString())
                    .messageAttributes(messageAttributes)
                    .build();
        }

        log.error("Sns message is null for JobsDetails {} from JobDiva for client {}", job.getId(), clientId);
        return null;
    }


    public <T> void enrichJobsWithContactInfo(List<Job> jobs, int clientId) {
        log.info("Started enriching {} JobsDetail from job diva with contact info for client {}", jobs.size(),
                clientId);
        Set<String> contactIds = jobs.stream()
                .map(Job::getContactId)
                .filter(contactId -> !contactId.isBlank() && !contactId.equalsIgnoreCase("0"))
                .collect(Collectors.toSet());
        if (!contactIds.isEmpty()) {
            Map<String, Integer> atsResult = curatelyRepository.getContactIdsByAtsValues(clientId,
                    new ArrayList<>(contactIds), JOB_DIVA);
            String entityIds = contactIds.stream()
                    .filter(id -> !atsResult.containsKey(id))
                    .collect(Collectors.joining(","));

            ResponseEntity<GenericResponse<List<T>>> response = fetchDetails(entityIds, CONTACTS_DETAIL, clientId);
            GenericResponse<List<T>> body = response.getBody();
            if (CommonUtils.isInvalidResponse(body)) {
                CommonUtils.handleErrorResponse(CONTACTS_DETAIL, clientId, response);
                return;
            }

            List<Contact> contacts = (List<Contact>) convertResponse(body, Contact.class);
            Map<String, Contact> contactMap = contacts.stream().collect(Collectors.toMap(
                    Contact::getContactid,
                    contact -> contact
            ));
            jobs.forEach(job -> {
                String contactId = job.getContactId();
                if (!contactId.isBlank() && !contactId.equalsIgnoreCase("0")
                        && !atsResult.containsKey(contactId)) {
                    job.setContact(contactMap.getOrDefault(contactId, null));
                }
            });
        }
    }


    private <T> void enrichCandidatesWithResume(List<Candidate> candidates, Set<String> candidateIds,
                                                int clientId) {

        log.error("Started enriching {} CandidatesDetail from job diva with resume id for client {}",
                candidateIds.size(), clientId);
        List<String> candidateIdsList = candidateIds.stream().toList();
        List<CandidateResume> allCandidateResumes = new ArrayList<>();
        for (int i = 0; i < candidateIdsList.size(); i += 100) {
            int end = Math.min(i + 100, candidateIdsList.size());
            List<String> batch = candidateIdsList.subList(i, end);

            ResponseEntity<GenericResponse<List<T>>> response = fetchDetails(String.join(",", batch),
                    CANDIDATE_RESUME_DETAIL, clientId);
            GenericResponse<List<T>> body = response.getBody();
            if (CommonUtils.isInvalidResponse(body)) {
                CommonUtils.handleErrorResponse(CANDIDATE_RESUME_DETAIL, clientId, response);
                return;
            }

            List<CandidateResume> candidateResumes = (List<CandidateResume>) convertResponse(body, CandidateResume.class);
            allCandidateResumes.addAll(candidateResumes);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        Map<String, CandidateResume> candidateResumeIdMap = allCandidateResumes.stream()
                .filter(candidateResume -> candidateResume.getDateUpdated() != null)
                .collect(Collectors.groupingBy(CandidateResume::getCandidateId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(candidateResume ->
                                        LocalDateTime.parse(candidateResume.getDateUpdated(), formatter))),
                                maxResume -> maxResume.orElse(null)
                        )
                ));
        candidates.forEach(candidate -> {
            CandidateResume candidateResume = candidateResumeIdMap.get(candidate.getCandidateid());
            if (candidateResume != null) {
                candidate.setResumeId(candidateResume.getResumeId());
            }
        });
    }

    private <T> List<String> getSourcedIds(int clientId, String jobId) {
        ResponseEntity<GenericResponse<List<T>>> response;
        GenericResponse<List<T>> body;
        response = clientService.jobAgentSearch(clientId, jobId, resumeCount);
        body = response.getBody();
        if (CommonUtils.isInvalidResponse(body)) {
            CommonUtils.handleErrorResponse("job agent search", clientId, response);
            return Collections.emptyList();
        }

        List<Candidate> sourcedCandidates = (List<Candidate>) convertResponse(body, Candidate.class);
        return sourcedCandidates.stream().map(Candidate::getCandidateid).toList();
    }

    public <T> Map<String, Job> getJobMap(int clientId, List<Hotlist> hotlistlistList, String entityName) {
        GenericResponse<List<T>> body;
        ResponseEntity<GenericResponse<List<T>>> response;
        List<String> jobIds = hotlistlistList.stream()
                .map(Hotlist::getJobId)
                .toList();

        response = fetchDetails(String.join(",", jobIds), JOB_DETAIL, clientId);
        body = response.getBody();
        if (CommonUtils.isInvalidResponse(body)) {
            CommonUtils.handleErrorResponse(entityName, clientId, response);
            return Collections.emptyMap();
        }

        List<Job> jobs = (List<Job>) convertResponse(body, Job.class);
        enrichJobsWithContactInfo(jobs, clientId);
        enrichJobsWithUsersInfo(jobs, clientId);
        return jobs.stream()
                .collect(Collectors.toMap(Job::getId, job -> job));
    }

    public <T> ResponseEntity<GenericResponse<List<T>>> fetchDetails(String ids, String entityName, int clientId) {
        return clientService.getDetails(ids, null, entityName, clientId);
    }

    public <T> List<T> convertResponse(GenericResponse<List<T>> body, Class<?> modelClass) {
        return objectMapper.convertValue(
                Objects.requireNonNull(body).getData(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, modelClass)
        );
    }

    public Map<String, Set<JobApplicant>> groupByJobId(List<JobApplicant> applicants) {
        return applicants.stream().collect(Collectors.groupingBy(
                JobApplicant::getJobId,
                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(JobApplicant::getCandidateId)))
        ));
    }

    private SnsEvent buildSnsEvent(String entityName, String entityId, int clientId, String dataString,
                                   int recruiterId, String eventType) {
        return SnsEvent.builder()
                .entityName(entityName)
                .entityId(entityId)
                .eventType(eventType)
                .clientId(clientId)
                .data(dataString)
                .updatedFields(null)
                .metadata(null)
                .atsName(JOB_DIVA)
                .recruiterId(recruiterId)
                .build();
    }

    private String convertModelToString(Object snsEvent) {
        try {
            return objectMapper.writeValueAsString(snsEvent);
        } catch (JsonProcessingException e) {
            log.error("Error converting sns event model to json string", e);
        }
        return null;
    }

    public <T> Map<String, List<String>> fetchUpdatedCandidateNotes(int clientId, Timestamp fromDate, Date toDate)
            throws DataAccessException {
        String formattedFromDate = formatDate(fromDate);
        String formattedToDate = formatDate(toDate);

        int pageNumber = 1;
        Map<String, List<String>> jobCandidateIdsMap = new HashMap<>();
        ResponseEntity<GenericResponse<List<T>>> response;
        GenericResponse<List<T>> body;
        while (true) {
            response = clientService.getNewUpdatedCandidateNotes(clientId, formattedFromDate, formattedToDate, pageNumber);
            body = response.getBody();
            if (CommonUtils.isInvalidResponse(body)) {
                CommonUtils.handleErrorResponse("NewUpdatedCandidateNotes", clientId, response);
                return Collections.emptyMap();
            }

            if (body.getData().isEmpty()) {
                log.error("Empty data from Job diva while fetching NewUpdatedCandidateNotes, client {} : {}", clientId,
                        response);
                break;
            }

            List<CandidateNote> candidateNotes = (List<CandidateNote>) convertResponse(body, CandidateNote.class);
            candidateNotes.stream()
                .filter(candidateNote -> StringUtils.isNotEmpty(candidateNote.getJobId()))
                .forEach(candidateNote -> jobCandidateIdsMap.computeIfAbsent(candidateNote.getJobId(), k -> new ArrayList<>())
                    .add(candidateNote.getCandidateId()));
            pageNumber++;
        }
        return jobCandidateIdsMap;
    }

    public Timestamp fetchFromDate(int clientId) throws DataAccessException {
        Timestamp fromDate = curatelyRepository.fetchLastSyncedTime(clientId);
        if (fromDate == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            fromDate = new Timestamp(calendar.getTimeInMillis());
            log.info("No last synced time found for client {}. Using fromDate as three hours back: {}", clientId,
                    fromDate);
        }
        return fromDate;
    }


    private String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        return dateFormat.format(date);
    }

    
    public PublishBatchRequestEntry processCandidateNotes(int clientId, int recruiterId,
            List<Applicant> applicants, String entityName,
            String entityId) throws IOException {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        String dataString = convertModelToString(applicants);

        SnsEvent snsEvent = buildSnsEvent(entityName, entityId, clientId, dataString, recruiterId, "Delete");
        String message = convertModelToString(snsEvent);
        if (message != null) {
            String base64EncodedString = CompressionUtil.compress(message);
            Map<String, MessageAttributeValue> localMessageAttributes = new HashMap<>(messageAttributes);
            localMessageAttributes.put("actionType", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue("deleteCandidatesFromCase")
                    .build());
            return PublishBatchRequestEntry.builder()
                    .message(base64EncodedString)
                    .id(UUID.randomUUID().toString())
                    .messageAttributes(localMessageAttributes)
                    .build();
        }
        log.error("Sns message is null for {} {} from JobDiva for client {}",
                applicants.size(), entityName, clientId);
        return null;
    }

    public PublishBatchRequestEntry processJobUpdate(int clientId, int recruiterId, Job job)
            throws IOException {
        job.setDoProcessManual(Boolean.TRUE);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        String dataString = convertModelToString(job);

        SnsEvent snsEvent = buildSnsEvent("Job", job.getId(), clientId, dataString, recruiterId,
                "Update");
        String message = convertModelToString(snsEvent);
        if (message != null) {
            String base64EncodedString = CompressionUtil.compress(message);
            return PublishBatchRequestEntry.builder()
                    .message(base64EncodedString)
                    .id(UUID.randomUUID().toString())
                    .messageAttributes(messageAttributes)
                    .build();
        }
        log.error("Sns message is null for Job Detail {} from JobDiva for client {}", job.getId(), clientId);
        return null;
    }



    public void saveJobsEvent(int clientId, int count, Timestamp fromDate, Date toDate) {
        curatelyRepository.saveJobsEventDetails(clientId, "NewUpdatedJobRecords", count, fromDate, toDate);
    }

    public <T> Map<String, Set<JobApplicant>> scheduleJobApplications(int clientId) {

        Map<Integer, String> resultMap = curatelyRepository.findJobAtsValuesForJobDivaRanked(clientId, Boolean.FALSE);
        if (resultMap.isEmpty()) {
            return Collections.emptyMap();
        }

        String existingJobIds = String.join(",", resultMap.values());
        ResponseEntity<GenericResponse<List<T>>> response = fetchDetails(existingJobIds, JOB_APPLICANT_DETAIL,
                clientId);
        GenericResponse<List<T>> body = response.getBody();

        if (CommonUtils.isInvalidResponse(body)) {
            CommonUtils.handleErrorResponse(JOB_APPLICANT_DETAIL, clientId, response);
            return Collections.emptyMap();
        }

        List<JobApplicant> applicants = (List<JobApplicant>) convertResponse(body, JobApplicant.class);
        Map<String, Set<JobApplicant>> groupedApplicants = groupByJobId(applicants);

        List<String> atsValues = groupedApplicants.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(applicant -> entry.getKey() + "-" + applicant.getCandidateId()))
                .collect(Collectors.toList());

        List<String> existingAtsValues = atsValues.isEmpty()
                ? Collections.emptyList()
                : curatelyRepository.findExistingAtsValues(clientId, atsValues);
        return groupedApplicants.entrySet().stream()
                .map(entry -> {
                    String jobId = entry.getKey();
                    Set<JobApplicant> filteredApplicants = entry.getValue().stream()
                            .filter(applicant -> !existingAtsValues.contains(jobId + "-" + applicant.getCandidateId()))
                            .collect(Collectors.toSet());
                    if (!filteredApplicants.isEmpty()) {
                        Set<String> candidateIds = filteredApplicants.stream()
                                .map(JobApplicant::getCandidateId)
                                .collect(Collectors.toSet());
                        List<String> filteredCandidates = filterCandidates(candidateIds, jobId, clientId);
                        filteredApplicants = filteredApplicants.stream()
                                .filter(applicant -> filteredCandidates.contains(applicant.getCandidateId()))
                                .collect(Collectors.toSet());
                    }
                    return Map.entry(jobId, filteredApplicants);
                })
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<String> getExistingJobAtsValues(int clientId) {
        Map<Integer, String> resultMap = curatelyRepository.findJobAtsValuesForJobDivaRanked(clientId, Boolean.TRUE);
        return resultMap.values().stream().toList();
    }

    public <T> List<Candidate> getNewJobAgentSearch(int clientId, String jobId) {

        List<String> sourcedIds = getSourcedIds(clientId, jobId);
        List<String> atsValues = sourcedIds.stream()
                .map(sourcedId -> jobId + "-" + sourcedId)
                .collect(Collectors.toList());
        List<String> existingAtsValues = atsValues.isEmpty()
                ? Collections.emptyList()
                : curatelyRepository.findAtsValuesInOpenResume(clientId, atsValues);

        sourcedIds = sourcedIds.stream()
                .filter(sourcedId -> !existingAtsValues.contains(jobId + "-" + sourcedId))
                .toList();
        if (sourcedIds.isEmpty()) {
            return Collections.emptyList();
        }

        ResponseEntity<GenericResponse<List<T>>> response = fetchDetails(String.join(",", sourcedIds),
                CANDIDATE_DETAIL, clientId);
        GenericResponse<List<T>> body = response.getBody();
        if (CommonUtils.isInvalidResponse(body)) {
            CommonUtils.handleErrorResponse(CANDIDATE_DETAIL, clientId, response);
            return Collections.emptyList();
        }

        List<Candidate> candidates = (List<Candidate>) convertResponse(body, Candidate.class);
        enrichCandidatesWithResume(candidates, new HashSet<>(sourcedIds), clientId);
        return candidates;
    }



    public <T> void enrichJobsWithUsersInfo(List<Job> jobs, int clientId) {
        log.info("Started enriching {} JobsDetail from job diva with users info for client {}", jobs.size(),
                clientId);
        String entityIds = jobs.stream()
                .map(Job::getId)
                .collect(Collectors.joining(","));
        ResponseEntity<GenericResponse<List<T>>> response = fetchDetails(entityIds, JOBS_USERS_DETAIL, clientId);
        GenericResponse<List<T>> body = response.getBody();
        if (CommonUtils.isInvalidResponse(body)) {
            CommonUtils.handleErrorResponse(JOBS_USERS_DETAIL, clientId, response);
            return;
        }

        List<Job.User> users = (List<Job.User>) convertResponse(body, Job.User.class);
        Map<String, List<Job.User>> usersByJobId = users.stream()
                .collect(Collectors.groupingBy(Job.User::getJobId));
        jobs.forEach(job -> {
            job.setUsers(usersByJobId.getOrDefault(job.getId(), Collections.emptyList()));
        });
    }

    private <T> List<String> getCandidatesWithNotesOnJob(Set<String> candidateIds, String jobId, int clientId) {
        ResponseEntity<GenericResponse<CandidateNotesListResponse>> notesResponse = clientService.getCandidateNotesList(clientId, String.join(",", candidateIds));
        GenericResponse<CandidateNotesListResponse> notesBody = notesResponse.getBody();

        if (notesBody == null || !notesBody.getSuccess() || notesBody.getData() == null) {
            return Collections.emptyList();
        }

        Map<String, List<CandidateNote>> data = notesBody.getData().getData();
        return data.keySet().stream()
                .filter(candidateId -> {
                    return data.get(candidateId).stream()
                            .anyMatch(note -> note.getJobId().equals(jobId));
                })
                .distinct()
                .collect(Collectors.toList());
    }

    public <T> List<String> filterCandidates(Set<String> candidateIds, String jobId, int clientId) {
        List<String> candidatesWithNotes = getCandidatesWithNotesOnJob(candidateIds, jobId, clientId);
        if (!candidatesWithNotes.isEmpty()) {
            log.info("Found {} candidates with notes for job {}", candidatesWithNotes.size(), jobId);
        } else {
            log.info("No candidates with notes found for job {}, client {}", jobId, clientId);
        }
        Set<String> candidatesToProcess = new HashSet<>(candidateIds);
        candidatesToProcess.removeAll(candidatesWithNotes);

        ResponseEntity<GenericResponse<List<T>>> submittalsResponse = clientService.getDetails(
                StringUtils.join(candidatesToProcess, ","), null, CANDIDATES_SUBMITTALS_DETAIL, clientId);
        ResponseEntity<GenericResponse<List<T>>> dnsResponse = clientService.getDetails(
                StringUtils.join(candidatesToProcess, ","), null,
                CANDIDATES_DO_NOT_SUBMIT_LIST_DETAIL, clientId);

        GenericResponse<List<T>> submittalsBody = submittalsResponse.getBody();
        GenericResponse<List<T>> dnsBody = dnsResponse.getBody();

        if (CommonUtils.isInvalidResponse(submittalsBody) || CommonUtils.isInvalidResponse(dnsBody)) {
            CommonUtils.handleErrorResponse(CANDIDATES_SUBMITTALS_DETAIL, clientId, submittalsResponse);
            CommonUtils.handleErrorResponse(CANDIDATES_DO_NOT_SUBMIT_LIST_DETAIL, clientId, dnsResponse);
            return Collections.emptyList();
        }

        List<CandidateSubmittal> candidatesSubmittals = (List<CandidateSubmittal>) convertResponse(submittalsBody,
                CandidateSubmittal.class);
        List<DoNotSubmit> doNotSubmits = (List<DoNotSubmit>) convertResponse(dnsBody, DoNotSubmit.class);

        Set<String> candidatesSubmittedAndActive = candidatesSubmittals.parallelStream()
                .filter(candidateSubmittal -> candidateSubmittal.getJobId().equalsIgnoreCase(jobId)
                        || isActiveCandidate(candidateSubmittal))
                .map(CandidateSubmittal::getCandidateId)
                .collect(Collectors.toSet());
        log.info("Candidates submitted and active: {}, jobId: {}, clientId: {}", candidatesSubmittedAndActive, jobId, clientId);

        Map<String, List<String>> candidateDnsMap = doNotSubmits.parallelStream()
                .collect(Collectors.groupingBy(
                        DoNotSubmit::getCandidateId,
                        Collectors.mapping(DoNotSubmit::getCompanyId, Collectors.toList())
                ));

        ResponseEntity<GenericResponse<List<T>>> jobResponse = fetchDetails(jobId, JOB_DETAIL, clientId);
        GenericResponse<List<T>> jobBody = jobResponse.getBody();

        if (CommonUtils.isInvalidResponse(jobBody)) {
            CommonUtils.handleErrorResponse(JOB_DETAIL, clientId, jobResponse);
            return Collections.emptyList();
        }

        List<Job> jobs = (List<Job>) convertResponse(jobBody, Job.class);
        String currentCompanyId = jobs.get(0).getCompanyId();

        return candidateIds.parallelStream()
                .filter(candidateId -> !candidatesSubmittedAndActive.contains(candidateId))
                .filter(candidateId -> {
                    List<String> dnsList = candidateDnsMap.get(candidateId);
                    boolean isValid = dnsList == null || (!dnsList.contains("0") && !dnsList.contains(currentCompanyId));
                    if (!isValid) {
                        log.info("Candidate {} filtered out due to DNS list containing 0 or company ID {}", candidateId, currentCompanyId);
                    }
                    return isValid;
                })
                .toList();
    }

    private boolean isActiveCandidate(CandidateSubmittal candidateSubmittal) {
        String startDate = candidateSubmittal.getStartDate();
        String terminationDate = candidateSubmittal.getTerminationDate();
        if (!startDate.isBlank()) {
            if (terminationDate.isBlank()) {
                return true;
            }
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime dateTime = LocalDateTime.parse(terminationDate, formatter);
            return dateTime.isAfter(LocalDateTime.now());
        }
        return false;
    }
}
