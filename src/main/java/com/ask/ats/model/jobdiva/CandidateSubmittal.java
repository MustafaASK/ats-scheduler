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
public class CandidateSubmittal {

    @JsonProperty("CANDIDATEID")
    private String candidateId;

    @JsonProperty("ID")
    private String id;

    @JsonProperty("USERID")
    private String userId;

    @JsonProperty("PRIMARYSALESID")
    private String primarySalesId;

    @JsonProperty("JOBID")
    private String jobId;

    @JsonProperty("CONTACTID")
    private String contactId;

    @JsonProperty("ROLEID")
    private String roleId;

    @JsonProperty("DATECREATED")
    private String dateCreated;

    @JsonProperty("DATEUPDATED")
    private String dateUpdated;

    @JsonProperty("SUBMITTALDATE")
    private String submittalDate;

    @JsonProperty("REJECTIONDATE")
    private String rejectionDate;

    @JsonProperty("DATEINTERVIEW")
    private String dateInterview;

    @JsonProperty("INTERVIEW_TIMEZONEID")
    private String interviewTimezoneId;

    @JsonProperty("STARTDATE")
    private String startDate;

    @JsonProperty("ENDDATE")
    private String endDate;

    @JsonProperty("TERMINATIONDATE")
    private String terminationDate;

    @JsonProperty("MANAGERFIRSTNAME")
    private String managerFirstName;

    @JsonProperty("MANAGERLASTNAME")
    private String managerLastName;

    @JsonProperty("TERMINATIONREASON")
    private String terminationReason;

    @JsonProperty("REJECTREASON")
    private String rejectReason;

    @JsonProperty("CORPTOCORP")
    private String corpToCorp;

    @JsonProperty("AGREEDBILLRATE")
    private String agreedBillRate;

    @JsonProperty("QUOTEDBILLRATE")
    private String quotedBillRate;

    @JsonProperty("BILLFREQUENCY")
    private String billFrequency;

    @JsonProperty("BILLRATECURRENCY")
    private String billRateCurrency;

    @JsonProperty("AGREEDPAYRATE")
    private String agreedPayRate;

    @JsonProperty("PAYFREQUENCY")
    private String payFrequency;

    @JsonProperty("CURRENCY")
    private String currency;

    @JsonProperty("FEE_TYPE")
    private String feeType;

    @JsonProperty("FEE_PERCENT")
    private String feePercent;

    @JsonProperty("FEE")
    private String fee;

    @JsonProperty("INTERNALSUBMITTALFLAG")
    private String internalSubmittalFlag;

    @JsonProperty("EXTERNALSUBMITTALFLAG")
    private String externalSubmittalFlag;

    @JsonProperty("NOTES")
    private String notes;

}
