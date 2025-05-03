package com.ask.ats.model.jobdiva;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * The type Search job.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchJob {

    @JsonProperty("city")
    private String city;

    @JsonProperty("companyId")
    private Integer companyId;

    @JsonProperty("companyname")
    private String companyName;

    @JsonProperty("contactid")
    private Integer contactId;

    @JsonProperty("countryId")
    private String countryId;

    @JsonProperty("divisionId")
    private Integer divisionId;

    @JsonProperty("divisionIds")
    private List<Integer> divisionIds;

    @JsonProperty("experience")
    private Integer experience;

    @JsonProperty("ismyjob")
    private boolean isMyJob;

    @JsonProperty("issuedatefrom")
    private String issueDateFrom;

    @JsonProperty("issuedateto")
    private String issueDateTo;

    @JsonProperty("jobId")
    private Integer jobId;

    @JsonProperty("jobdivaref")
    private String jobDivARef;

    @JsonProperty("jobtype")
    private List<String> jobType;

    @JsonProperty("maxReturned")
    private Integer maxReturned;

    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("onlyHealthcareJob")
    private boolean onlyHealthcareJob;

    @JsonProperty("optionalref")
    private String optionalRef;

    @JsonProperty("portalId")
    private Integer portalId;

    @JsonProperty("startdatefrom")
    private String startDateFrom;

    @JsonProperty("startdateto")
    private String startDateTo;

    @JsonProperty("state")
    private List<String> state;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("title")
    private String title;

    @JsonProperty("zipcode")
    private String zipCode;

    @JsonProperty("zipcodeRadius")
    private Integer zipCodeRadius;

}
