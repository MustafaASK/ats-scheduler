package com.ask.ats.model.jobdiva;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CandidateResume {
    @JsonProperty("CANDIDATEID")
    private String candidateId;

    @JsonProperty("DOCID")
    private String docId;

    @JsonProperty("DBID")
    private String dbId;

    @JsonProperty("RESUMEID")
    private String resumeId;

    @JsonProperty("DATECREATED")
    private String dateCreated;

    @JsonProperty("DATEUPDATED")
    private String dateUpdated;

    @JsonProperty("DATEFIRSTDOWNLOADED")
    private String dateFirstDownloaded;

    @JsonProperty("DATELASTDOWNLOADED")
    private String dateLastDownloaded;
}
