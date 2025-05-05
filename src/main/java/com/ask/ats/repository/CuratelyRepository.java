package com.ask.ats.repository;

import com.ask.ats.model.Applicant;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.ask.ats.utils.Constants.CURATELY;


/**
 * The type Curately repository.
 */
@Repository
@Log4j2
public class CuratelyRepository {

    @Value("${jobDiva.resumeCount}")
    private int resumeCount;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Instantiates a new Curately repository.
     *
     * @param jdbcTemplate               the jdbc template
     * @param namedParameterJdbcTemplate the named parameter jdbc template
     */
    public CuratelyRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }


    private static String replaceDbName(int clientId, String sql) {
        return sql.replace("DB_NAME", CURATELY + clientId);
    }

    public Timestamp fetchLastSyncedTime(int clientId) throws DataAccessException {
        String sql = "SELECT TOP 1 toDate FROM DB_NAME.dbo.JobDivaEvents WITH(NOLOCK) WHERE eventStatus = 1 "
                + "ORDER BY createdDate DESC";
        try {
            return jdbcTemplate.queryForObject(replaceDbName(clientId, sql), Timestamp.class);
        } catch (EmptyResultDataAccessException e) {
            log.info("No entry found for client {} in JobDivaEvents", clientId);
            return null;
        }
    }


    public Map<String, Integer> getContactIdsByAtsValues(int clientId, List<String> atsValues, String atsName)
            throws DataAccessException {
        String sql = "SELECT contid, atsvalue FROM DB_NAME.dbo.contact_ats WHERE atsvalue IN (:atsValues) "
                + "AND atsname=:atsName";
        MapSqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("atsValues", atsValues)
                .addValue("atsName", atsName);
        try {
            List<Map.Entry<String, Integer>> entries = namedParameterJdbcTemplate.query(replaceDbName(clientId, sql),
                    parameterSource,
                    (ResultSet rs, int rowNum) ->
                            Map.entry(rs.getString("atsvalue"), rs.getInt("contid"))
            );
            return entries.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (EmptyResultDataAccessException e) {
            log.info("No contact ats entries found for ats values {}, client {}", atsValues, clientId);
            return Collections.emptyMap();
        }
    }


    public void saveJobsEventDetails(int clientId, String eventName, int eventsCount, Timestamp fromDate,
                                     Date toDate) throws DataAccessException {
        String sql = "INSERT INTO DB_NAME.dbo.JobDivaEvents (eventName, eventsCount, eventStatus, fromDate, toDate, "
                + "createdDate) VALUES (:eventName, :eventsCount, :eventStatus, :fromDate, :toDate, :createdDate)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("eventName", eventName)
                .addValue("eventsCount", eventsCount)
                .addValue("eventStatus", 1)
                .addValue("fromDate", fromDate)
                .addValue("toDate", new Timestamp(toDate.getTime()))
                .addValue("createdDate", new Timestamp(System.currentTimeMillis()));

        int rowsAffected = namedParameterJdbcTemplate.update(replaceDbName(clientId, sql), params);
        if (rowsAffected > 0) {
            log.info("JobDivaEvents inserted successfully for client {}, event {}, count {}", clientId,
                    eventName, eventsCount);
        } else {
            log.error("Error inserting JobDivaEvents for client {}, event {}, count {}", clientId, eventName,
                    eventsCount);
        }
    }


    public Map<Integer, String> findJobAtsValuesForJobDivaRanked(int clientId, boolean useOpenResumes)
            throws DataAccessException {

        String sql = getJobAtsSql(useOpenResumes);
        try {
            List<Map.Entry<Integer, String>> entries = namedParameterJdbcTemplate.query(
                    replaceDbName(clientId, sql),
                    new MapSqlParameterSource(),
                    (ResultSet rs, int rowNum) ->
                            Map.entry(rs.getInt("jobid"), rs.getString("atsvalue"))
            );

            return entries.stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (EmptyResultDataAccessException e) {
            log.info("No job ats entries found for JobDiva atsName with open jobs for client {}", clientId);
            return Collections.emptyMap();
        }
    }

    private String getJobAtsSql(boolean useOpenResumes) {
        String tableName = useOpenResumes ? "openresumes" : "applicants";
        return "SELECT DISTINCT ja.jobid, ja.atsvalue FROM DB_NAME.dbo.job_ats ja WITH(NOLOCK) "
                + "INNER JOIN DB_NAME.dbo.job j WITH(NOLOCK) "
                + "ON ja.jobid = j.jobid "
                + "WHERE ja.atsname = 'JobDiva' AND j.status = '1' AND j.isDelete = 0 "
                + "AND EXISTS (SELECT 1 FROM DB_NAME.dbo." + tableName + " ors "
                + "WHERE ors.jobid = ja.jobid HAVING COUNT(*) < " + resumeCount + ")";
    }

    public List<String> findExistingAtsValues(int clientId, List<String> atsValue) throws DataAccessException {
        String sql = "SELECT DISTINCT atsvalue FROM DB_NAME.dbo.applicants_ats WITH(NOLOCK) "
                + "WHERE atsvalue IN (:atsValue) AND atsname = 'JobDiva'";
        MapSqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("atsValue", atsValue);

        try {
            return namedParameterJdbcTemplate.queryForList(replaceDbName(clientId, sql), parameterSource, String.class);
        } catch (EmptyResultDataAccessException e) {
            log.info("No matching ATS values found in applicants_ats for client {}", clientId);
            return Collections.emptyList();
        }
    }

    public List<String> findAtsValuesInOpenResume(int clientId, List<String> atsValue) throws DataAccessException {
        String sql = "SELECT DISTINCT atsvalue FROM DB_NAME.dbo.openresumes_ats WITH(NOLOCK) "
                + "WHERE atsvalue IN (:atsValue) AND atsname = 'JobDiva'";
        MapSqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("atsValue", atsValue);

        try {
            return namedParameterJdbcTemplate.queryForList(replaceDbName(clientId, sql), parameterSource, String.class);
        } catch (EmptyResultDataAccessException e) {
            log.info("No matching ATS values found in openresumes_ats for client {}", clientId);
            return Collections.emptyList();
        }
    }

    public List<String> getJobAtsValuesForAgileOne(int clientId, String atsName) {
        String sql = "SELECT DISTINCT atsValue FROM DB_NAME.dbo.job_ats where atsName = :atsName";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("atsName", atsName);
        return namedParameterJdbcTemplate.query(replaceDbName(clientId, sql), params, (rs, rowNum) -> rs.getString("atsValue"));
    }

    public List<String> getAssignmentAtsValuesForAgileOne(int clientId, String atsName) {
        String sql = "SELECT DISTINCT atsValue FROM DB_NAME.dbo.openresumes_ats where atsName = :atsName";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("atsName", atsName);
        return namedParameterJdbcTemplate.query(replaceDbName(clientId, sql), params, (rs, rowNum) -> rs.getString("atsValue"));
    }

    public LocalDateTime getLatestLastActivityDate(int clientId, String type) {
        String sql = "SELECT TOP 1 lastActivityDate FROM DB_NAME.dbo.scheduleJobActivity "
                + "WHERE type = :type AND status = 1 ORDER BY lastActivityDate DESC";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("type", type);

        try {
            return namedParameterJdbcTemplate.queryForObject(replaceDbName(clientId, sql), params, (rs, rowNum) ->
                    rs.getTimestamp("lastActivityDate").toLocalDateTime());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void saveLastActivityDate(int clientId, String type, LocalDateTime lastActivityDate, int eventsCount) {
        String insertSql = "INSERT INTO DB_NAME.dbo.scheduleJobActivity (type, eventsCount, status, reason, "
                + "lastActivityDate, createdDate) "
                + "VALUES (:type, :eventsCount, :status, :reason, :lastActivityDate, GETDATE())";
        MapSqlParameterSource insertParams = new MapSqlParameterSource()
                .addValue("type", type)
                .addValue("eventsCount", eventsCount)
                .addValue("status", Boolean.TRUE)
                .addValue("reason", null)
                .addValue("lastActivityDate", Timestamp.valueOf(lastActivityDate));

        int rowsAffected = namedParameterJdbcTemplate.update(replaceDbName(clientId, insertSql), insertParams);
        if (rowsAffected > 0) {
            log.info("Last activity date successfully inserted for clientId: {}", clientId);
        } else {
            log.error("Error inserting last activity date for clientId: {}", clientId);
        }
    }

    public List<Integer> fetchClientIdsByAts(Integer atsId) throws DataAccessException {
        String sql = "SELECT DISTINCT clientid FROM curately.dbo.settings s "
                + "INNER JOIN curately.dbo.admin_master_modules amm ON (amm.id = s.settingsid) "
                + "WHERE s.settingsid = :atsId;";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("atsId", atsId);

        try {
            return namedParameterJdbcTemplate.query(sql, params,
                    (rs, rowNum) -> rs.getInt("clientid"));
        } catch (EmptyResultDataAccessException e) {
            log.info("No client details found for ats id {}", atsId);
            return Collections.emptyList();
        }
    }


    public List<Applicant> filterVoiceAiApplicants(int clientId, List<String> atsValues) throws DataAccessException {
        String sql = "SELECT DISTINCT a.applicantid, app.jobid, app.userid " +
                "FROM DB_NAME.dbo.applicants_ats a " +
                "INNER JOIN DB_NAME.dbo.applicants_ats b ON a.applicantid = b.applicantid " +
                "INNER JOIN DB_NAME.dbo.applicants app ON a.applicantid = app.applicantid " +
                "WHERE a.atsname = 'VoiceAI' " +
                "AND a.isSuccess = 1 " +
                "AND b.atsname = 'JobDiva' " +
                "AND b.atsvalue IN (:atsValues)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("atsValues", atsValues);
        try {
            return namedParameterJdbcTemplate.query(replaceDbName(clientId, sql), params, (rs, rowNum) ->
                    Applicant.builder()
                            .applicantId(rs.getInt("applicantid"))
                            .jobId(rs.getInt("jobid"))
                            .userId(rs.getInt("userid"))
                            .build());
        } catch (EmptyResultDataAccessException e) {
            log.info("No voice ai applicants found for client {}", clientId);
            return Collections.emptyList();
        }
    }

    public List<Applicant> filterVoiceAiOpenResumes(int clientId, List<String> atsValues) throws DataAccessException {
        String sql = "SELECT DISTINCT a.openid, app.jobid, app.userid " +
                "FROM DB_NAME.dbo.openresumes_ats a " +
                "INNER JOIN DB_NAME.dbo.openresumes_ats b ON a.openid = b.openid " +
                "INNER JOIN DB_NAME.dbo.openresumes app ON a.openid = app.openid " +
                "WHERE a.atsname = 'VoiceAI' " +
                "AND a.isSuccess = 1 " +
                "AND b.atsname = 'JobDiva' " +
                "AND b.atsvalue IN (:atsValues)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("atsValues", atsValues);
        try {
            return namedParameterJdbcTemplate.query(replaceDbName(clientId, sql), params, (rs, rowNum) ->
                    Applicant.builder()
                            .openId(rs.getInt("openid"))
                            .jobId(rs.getInt("jobid"))
                            .userId(rs.getInt("userid"))
                            .build());
        } catch (EmptyResultDataAccessException e) {
            log.info("No voice ai open resumes found for client {}", clientId);
            return Collections.emptyList();
        }
    }

    public void insertEventSubscriptionList(String subscriptionId, Integer requestId,
                                            int eventCount, String eventsJson, int clientId, Boolean eventStatus) {
        String sql = "INSERT INTO DB_NAME.dbo.SubscriptionListEvents (subscriptionId, requestId, "
                + "eventsCount, eventStatus, json) VALUES (?, ?, ?, ?, ?)";
        int rowsAffected = jdbcTemplate.update(replaceDbName(clientId, sql), subscriptionId, requestId, eventCount,
                eventStatus, eventsJson);
        if (rowsAffected > 0) {
            log.info("Subscription {} events list inserted successfully for request {}, client {}", subscriptionId,
                    requestId, clientId);
        } else {
            log.error("Error inserting subscription {} events list for request {}, client {}", subscriptionId,
                    requestId, clientId);
        }
    }


    public void updateEventSubscriptionList(String subscriptionId, int clientId, int requestId)
            throws DataAccessException {
        String sql = "UPDATE DB_NAME.dbo.SubscriptionListEvents SET eventStatus = 1, updatedDate = GETDATE() "
                + "WHERE subscriptionId = ? AND requestId = ?";

        int rowsAffected = jdbcTemplate.update(replaceDbName(clientId, sql), subscriptionId, requestId);
        if (rowsAffected > 0) {
            log.info("Subscription {} events list updated successfully for request {}, client {}", subscriptionId,
                    requestId, clientId);
        } else {
            log.error("Error updating subscription {} events list for request {}, client {}", subscriptionId,
                    requestId, clientId);
        }
    }

    public String getActiveSubscriptionId(int clientId, int recruiterId) throws DataAccessException {
        String sql = "SELECT TOP 1 subscriptionId FROM DB_NAME.dbo.SubscriptionEvent WHERE recruiterId = ? "
                + "AND subscriptionStatus = 1 ORDER BY id DESC";
        try {
            return jdbcTemplate.queryForObject(replaceDbName(clientId, sql), String.class, recruiterId);
        } catch (EmptyResultDataAccessException e) {
            log.info("No active subscription found for recruiter {} in client {}", recruiterId, clientId);
            return "";
        }
    }


}
