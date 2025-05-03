package com.ask.ats.config;

import com.ask.ats.repository.CuratelyRepository;
import com.ask.ats.service.AsyncService;
import com.ask.ats.service.ClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The type Scheduled task config.
 */
@Configuration
@Slf4j
@EnableScheduling
public class ScheduledTaskConfig implements SchedulingConfigurer {

    @Value("${scheduled.bullhorn.cronExpression}")
    private String cronExpression;

    @Value("${scheduled.agileOne.job.cronExpression}")
    private String agileOneJobCronExpression;

    @Value("${scheduled.agileOne.assignment.cronExpression}")
    private String agileOneAssignmentCronExpression;

    @Value("${scheduled.bullhorn.enabled}")
    private Boolean isEnabled;

    @Value("${scheduled.bullhorn.recruiter}")
    private int bullhornRecruiter;

    @Value("${scheduled.agileOne.assignment.enabled}")
    private Boolean isAgileOneAssignmentEnabled;

    @Value("${scheduled.agileOne.job.enabled}")
    private Boolean isAgileOneJobEnabled;

    @Value("${scheduled.jobDiva.jobAgentSearch.cronExpression}")
    private String jobAgentCronExpression;

    @Value("${scheduled.jobDiva.jobAgentSearch.enabled}")
    private Boolean isJobsAgentSearchEnabled;

    @Value("${scheduled.jobDiva.recruiter}")
    private int jobDivaRecruiter;

    @Value("${scheduled.agileOne.client}")
    private int agileOneClient;

    @Value("${scheduled.agileOne.recruiter}")
    private int agileOneRecruiter;

    @Value("${scheduled.jobDiva.applicants.enabled}")
    private Boolean isJobApplicantsEnabled;

    @Value("${scheduled.jobDiva.applicants.cronExpression}")
    private String jobApplicantCronExpression;

    @Value("${curately.jobDiva.atsId}")
    private Integer jobDivaAtsId;

    @Value("${curately.bullhorn.atsId}")
    private Integer bullhornAtsId;

    @Value("${scheduled.jobDiva.candidateNotes.enabled}")
    private Boolean isCandidateNotesEnabled;

    @Value("${scheduled.jobDiva.candidateNotes.cronExpression}")
    private String candidateNotesCornExpressionInSec;

    private final AsyncService asyncService;
    private final CuratelyRepository curatelyRepository;
    private final ClientService clientService;


    @Autowired
    public ScheduledTaskConfig(AsyncService asyncService, CuratelyRepository curatelyRepository, ClientService clientService) {
        this.asyncService = asyncService;
        this.curatelyRepository = curatelyRepository;
        this.clientService = clientService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (Boolean.TRUE.equals(isEnabled)) {
            taskRegistrar.addCronTask(this::fetchEventsOnSchedule, cronExpression);
        }

        if (Boolean.TRUE.equals(isJobsAgentSearchEnabled)) {
            taskRegistrar.addCronTask(this::updateJobsAgentSearchOnSchedule, jobAgentCronExpression);
        }

        if (Boolean.TRUE.equals(isJobApplicantsEnabled)) {
            taskRegistrar.addCronTask(this::updateJobApplicantsOnSchedule, jobApplicantCronExpression);
        }
        if (Boolean.TRUE.equals(isAgileOneJobEnabled)) {
            taskRegistrar.addCronTask(this::getListOfJobAgileOne, agileOneJobCronExpression);
        }
        if (Boolean.TRUE.equals(isAgileOneAssignmentEnabled)) {
            taskRegistrar.addCronTask(this::getListOfAssignmentAgileOne, agileOneAssignmentCronExpression);
        }
        if (Boolean.TRUE.equals(isCandidateNotesEnabled)) {
            taskRegistrar.addCronTask(this::fetchLatestCandidateNotes, candidateNotesCornExpressionInSec);
        }
    }

    private void fetchEventsOnSchedule() throws DataAccessException {
        try {
            log.info("Scheduled task started to process bullhorn events.");
            List<Integer> clientIds = curatelyRepository.fetchClientIdsByAts(bullhornAtsId);
            clientIds.forEach(clientId -> {
                clientService.fetchListOfEvents(clientId, bullhornRecruiter);
            });
            log.info("Scheduled task completed to process bullhorn events.");
        } catch (DataAccessException e) {
            log.error("SQL error while fetching active bullhorn event subscription", e);
        } catch (Exception e) {
            log.error("Unknown error while processing bullhorn events", e);
        }
    }

    private void updateJobsAgentSearchOnSchedule() {
        try {
            log.info("Scheduled task started to update job diva agent search.");
            List<Integer> clientIds = curatelyRepository.fetchClientIdsByAts(jobDivaAtsId);
            clientIds.forEach(clientId -> {
                asyncService.processJobsAgentSearch(clientId, jobDivaRecruiter);
            });
            log.info("Scheduled task completed to update job diva agent search for clients {}", clientIds);
        } catch (Exception e) {
            log.error("Unknown error while running job diva agent search schedule", e);
        }
    }

    private void updateJobApplicantsOnSchedule() {
        try {
            log.info("Scheduled task started to update job diva job applicants.");
            List<Integer> clientIds = curatelyRepository.fetchClientIdsByAts(jobDivaAtsId);
            clientIds.forEach(clientId -> {
                asyncService.processJobApplicationsAsync(clientId, jobDivaRecruiter);
            });
            log.info("Scheduled task completed to update job diva job applicants for clients {}", clientIds);
        } catch (Exception e) {
            log.error("Unknown error while running job diva jobs schedule", e);
        }
    }

    private void fetchLatestCandidateNotes() {
        try {
            log.info("Scheduled task started to fetch latest candidate notes.");
            List<Integer> clientIds = curatelyRepository.fetchClientIdsByAts(jobDivaAtsId);
            clientIds.forEach(clientId -> {
                asyncService.processNewCandidateNotes(clientId, jobDivaRecruiter);
            });
            log.info("Scheduled task completed to fetch latest candidate notes for clients {}", clientIds);
        } catch (Exception e) {
            log.error("Unknown error while running candidate notes scheduler", e);
        }
    }

    private void getListOfJobAgileOne() {
        try {
            log.info("Scheduled task started to fetch jobs from agileOne.");
            String formattedDate = getYesterdayDateFormatted();
            asyncService.processAgileOneJobs(agileOneClient, agileOneRecruiter, formattedDate, Boolean.TRUE);
            log.info("Scheduled task completed for getting jobs from agileOne.");
        } catch (Exception e) {
            log.error("Unknown error while running agileOne jobs scheduler", e);
        }
    }

    private void getListOfAssignmentAgileOne() {
        try {
            log.info("Scheduled task started to fetch assignments from agileOne.");
            String formattedDate = getYesterdayDateFormatted();
            asyncService.processAgileOneAssignments(agileOneClient, formattedDate, agileOneRecruiter, Boolean.TRUE);
            log.info("Scheduled task completed for getting assignments from agileOne.");
        } catch (Exception e) {
            log.error("Unknown error while running agileOne assignments scheduler", e);
        }
    }

    private String getYesterdayDateFormatted() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return yesterday.format(formatter);
    }

}
