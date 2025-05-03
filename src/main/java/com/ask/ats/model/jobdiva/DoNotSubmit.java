package com.ask.ats.model.jobdiva;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoNotSubmit {

    @JsonProperty("CANDIDATEID")
    private String candidateId;

    @JsonProperty("COMPANYID")
    private String companyId;

    @JsonProperty("REASON")
    private String reason;

    @JsonProperty("NOTE")
    private String note;

    @JsonProperty("DATEADDED")
    private String dateAdded;

    @JsonProperty("USERID")
    private String userId;

}
