package com.ask.ats.model.jobdiva;

import com.ask.ats.model.Address;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Job applicant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JobApplicant {

    @JsonProperty("CANDIDATEID")
    private String candidateId;

    @JsonProperty("FIRSTNAME")
    private String firstName;

    @JsonProperty("LASTNAME")
    private String lastName;

    @JsonProperty("EMAIL")
    private String email;

    @JsonProperty("JOBID")
    private String jobId;

    @JsonProperty("DATEAPPLIED")
    private String dateApplied;

    @JsonProperty("RESUMESOURCE")
    private String resumeSource;

    @JsonProperty("ACTION")
    private String action;

    @JsonProperty("ACTIONUSERID")
    private String actionUserId;

    @JsonProperty("ACTIONDATE")
    private String actionDate;

    @JsonProperty("REFERRER")
    private String referrer;

    @JsonProperty("RESUMEID")
    private String resumeId;

    @JsonProperty("STATUS")
    private String status;

    private String cellPhone;

    private Address address;

}

