package com.ask.ats.model.jobdiva;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CandidateNote {
    @JsonProperty("NOTEID")
    private String noteId;
    @JsonProperty("ACTIONTYPE")
    private String actionType;
    @JsonProperty("USERID")
    private String userId;
    @JsonProperty("CREATEDATE")
    private LocalDateTime createdDate;
    @JsonProperty("JOBID")
    private String jobId;
    @JsonProperty("CONTACTID")
    private String contactId;
    @JsonProperty("NOTE_TYPE")
    private String noteType;
    @JsonProperty("NOTE")
    private String note;
    @JsonProperty("CANDIDATEID")
    private String candidateId;
}
