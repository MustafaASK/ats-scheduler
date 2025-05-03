package com.ask.ats.model.jobdiva;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The type Hotlist.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hotlist {

    @JsonAlias({"HOTLIST_ID", "HOTLISTID"})
    private String hotListId;

    @JsonProperty("CANDIDATEID")
    private String candidateId;

    @JsonProperty("HOTLIST_NAME")
    private String hotListName;

    @JsonProperty("ACTIVE")
    private String active;

    @JsonProperty("DATECREATED")
    private String dateCreated;

    @JsonProperty("JOBID")
    private String jobId;

    @JsonProperty("CREATEDBY")
    private String createdBy;

    @JsonProperty("LINK_TO_JOB")
    private String linkToJob;

    @JsonProperty("LINK_TO_HIRINGMANAGER")
    private String linkToHiringManager;

    @JsonProperty("DESCRIPTION")
    private String description;

    @JsonProperty("USER_IDS")
    private List<Integer> userIds;

    @JsonProperty("GROUP_IDS")
    private List<Integer> groupIds;

    @JsonProperty("DIVISION_IDS")
    private List<Integer> divisionIds;

    private List<Candidate> candidates;

    private Job job;

}
