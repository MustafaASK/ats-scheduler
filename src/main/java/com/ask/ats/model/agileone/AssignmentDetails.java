package com.ask.ats.model.agileone;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Assignment details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentDetails {

    @JsonProperty("assignmentID")
    private int assignmentID;

    @JsonProperty("xRefCode")
    private String xRefCode;

    @JsonProperty("assignmentStatus")
    private String assignmentStatus;

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("lastActivityDate")
    private String lastActivityDate;

    @JsonProperty("jobID")
    private int jobId;

    @JsonProperty("jobStatus")
    private String jobStatus;

    @JsonProperty("applicantID")
    private int applicantId;
}