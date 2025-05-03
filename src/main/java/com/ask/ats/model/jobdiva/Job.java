package com.ask.ats.model.jobdiva;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The type Job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Job {

    @JsonAlias({"ID", "JOBID"})
    private String id;

    @JsonProperty("DATEISSUED")
    private String dateIssued;

    @JsonProperty("DATEUPDATED")
    private String dateUpdated;

    @JsonProperty("DATEUSERFIELDUPDATED")
    private String dateUserFieldUpdated;

    @JsonProperty("DATESTATUSUPDATED")
    private String dateStatusUpdated;

    @JsonProperty("JOBSTATUS")
    private String jobStatus;

    @JsonProperty("CUSTOMERID")
    private String customerId;

    @JsonProperty("COMPANYID")
    private String companyId;

    @JsonProperty("COMPANYNAME")
    private String companyName;

    @JsonProperty("ADDRESS1")
    private String address1;

    @JsonProperty("ADDRESS2")
    private String address2;

    @JsonProperty("UPDATEDBY")
    private String updatedBy;

    @JsonProperty("CITY")
    private String city;

    @JsonProperty("ZIPCODE")
    private String zipCode;

    @JsonProperty("SUBMITTAL_DUE")
    private String submittalDue;

    @JsonProperty("HARVEST")
    private String harvest;

    @JsonProperty("VMS_WEBSITE")
    private String vmsWebsite;

    @JsonProperty("CREATED_BY")
    private String createdBy;

    @JsonProperty("PRIORITY")
    private String priority;

    @JsonProperty("DIVISION")
    private String division;

    @JsonProperty("REFNO")
    private String refNo;

    @JsonProperty("JOBDIVANO")
    private String jobDivaNo;

    @JsonProperty("STARTDATE")
    private String startDate;

    @JsonProperty("ENDDATE")
    private String endDate;

    @JsonProperty("POSITIONS")
    private String positions;

    @JsonProperty("FILLS")
    private String fills;

    @JsonProperty("MAXALLOWEDSUBMITTALS")
    private String maxAllowedSubmittals;

    @JsonProperty("BILLRATEMIN")
    private String billRateMin;

    @JsonProperty("BILLRATEMAX")
    private String billRateMax;

    @JsonProperty("BILLRATEPER")
    private String billRatePer;

    @JsonProperty("PAYRATEMIN")
    private String payRateMin;

    @JsonProperty("PAYRATEMAX")
    private String payRateMax;

    @JsonProperty("PAYRATEPER")
    private String payRatePer;

    @JsonProperty("POSITIONTYPE")
    private String positionType;

    @JsonProperty("SKILLS")
    private String skills;

    @JsonProperty("JOBTITLE")
    private String jobTitle;

    @JsonProperty("JOBDESCRIPTION")
    private String jobDescription;

    @JsonProperty("REMARKS")
    private String remarks;

    @JsonProperty("SUBMITTALINSTRUCTION")
    private String submittalInstruction;

    @JsonProperty("POSTTOPORTAL")
    private String postToPortal;

    @JsonProperty("POSTING_TITLE")
    private String postingTitle;

    @JsonProperty("POSTING_DATE")
    private String postingDate;

    @JsonProperty("POSTINGDESCRIPTION")
    private String postingDescription;

    @JsonProperty("POSTING_CITY")
    private String postingCity;

    @JsonProperty("POSTING_STATE")
    private String postingState;

    @JsonProperty("POSTING_ZIPCODE")
    private String postingZipCode;

    @JsonProperty("POSTING_COUNTRY")
    private String postingCountry;

    @JsonProperty("REQUIRED_COUNTRY")
    private String requiredCountry;

    @JsonProperty("REQUIRED_STATE")
    private String requiredState;

    @JsonProperty("REQUIRED_AREACODES")
    private String requiredAreaCodes;

    @JsonProperty("REQUIRED_ZIPCODE")
    private String requiredZipCode;

    @JsonProperty("REQUIRED_WITHIN")
    private String requiredWithin;

    @JsonProperty("REQUIRED_PAYRANGE_FROM")
    private String requiredPayRangeFrom;

    @JsonProperty("REQUIRED_PAYRANGE_TO")
    private String requiredPayRangeTo;

    @JsonProperty("REQUIRED_PAYRANGE_PER")
    private String requiredPayRangePer;

    @JsonProperty("REQUIRED_MAJOR")
    private String requiredMajor;

    @JsonProperty("REQUIRED_DEGREE")
    private String requiredDegree;

    @JsonProperty("CRITERIA_DEGREE")
    private String criteriaDegree;

    @JsonProperty("JOBCATALOGID")
    private String jobCatalogId;

    @JsonProperty("CATALOGCOMPANYID")
    private String catalogCompanyId;

    @JsonProperty("CATALOGTITLE")
    private String catalogTitle;

    @JsonProperty("CATALOGREFNO")
    private String catalogRefNo;

    @JsonProperty("CATALOGNAME")
    private String catalogName;

    @JsonProperty("CATALOGACTIVE")
    private String catalogActive;

    @JsonProperty("CATALOGEFFECTIVEDATE")
    private String catalogEffectiveDate;

    @JsonProperty("CATALOGEXPIRATIONDATE")
    private String catalogExpirationDate;

    @JsonProperty("CATALOGCATEGORY")
    private String catalogCategory;

    @JsonProperty("CATALOGBILLRATELOW")
    private String catalogBillRateLow;

    @JsonProperty("CATALOGBILLRATEHIGH")
    private String catalogBillRateHigh;

    @JsonProperty("CATALOGBILLRATEPER")
    private String catalogBillRatePer;

    @JsonProperty("CATALOGPAYRATELOW")
    private String catalogPayRateLow;

    @JsonProperty("CATALOGPAYRATEHIGH")
    private String catalogPayRateHigh;

    @JsonProperty("CATALOGPAYRATEPER")
    private String catalogPayRatePer;

    @JsonProperty("POSITIONREFNO")
    private String positionRefNo;

    @JsonProperty("PREVENTLOWERPAY")
    private String preventLowerPay;

    @JsonProperty("PREVENTHIGHERBILL")
    private String preventHigherBill;

    @JsonProperty("CATALOGNOTES")
    private String catalogNotes;

    @JsonProperty("OT")
    private String ot;

    @JsonProperty("REFERENCES")
    private String references;

    @JsonProperty("TRAVEL")
    private String travel;

    @JsonProperty("DRUG_TEST")
    private String drugTest;

    @JsonProperty("BACKGROUND_CHECK")
    private String backgroundCheck;

    @JsonProperty("SECURITY_CLEARANCE")
    private String securityClearance;

    @JsonProperty("ONSITE_FLEXIBILITY")
    private String onsiteFlexibility;

    @JsonProperty("REMOTE_PERCENTAGE")
    private String remotePercentage;

    @JsonProperty("FEE")
    private String fee;

    @JsonProperty("FEE_TYPE")
    private String feeType;

    @JsonProperty("JOB_CATEGORY")
    private String jobCategory;

    @JsonProperty("PAYRATECANDIDATEPORTAL")
    private String payRateCandidatePortal;

    @JsonProperty("HEALTHCARE")
    private String healthcare;

    @JsonProperty("CURRENCY")
    private String currency;

    @JsonProperty("DIVISIONID")
    private String divisionId;

    @JsonProperty("DIVISIONNAME")
    private String divisionName;

    @JsonProperty("CONTACTID")
    private String contactId;

    @JsonProperty("CONTACTFIRSTNAME")
    private String contactFirstName;

    @JsonProperty("CONTACTLASTNAME")
    private String contactLastName;

    @JsonProperty("PRIMARYRECRUITERID")
    private String primaryRecruiterId;

    @JsonProperty("PRIMARYSALESID")
    private String primarySalesId;

    @JsonProperty("PRIMARYOWNERID")
    private String primaryOwnerId;

    @JsonProperty("EEOC_FEDERAL_SECTOR_OCCUPATION")
    private String eeocFederalSectorOccupation;

    @JsonProperty("STATE")
    private String state;

    @JsonProperty("COUNTRY")
    private String country;

    private List<JobApplicant> applicants;
    private List<Candidate> sourcedCandidates;
    private Contact contact;
    private Boolean doProcessManual;

    @JsonProperty("USERS")
    private List<User> users;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class User {
        @JsonProperty("JOBID")
        private String jobId;

        @JsonProperty("USERID")
        private String userId;

        @JsonProperty("FIRSTNAME")
        private String firstName;

        @JsonProperty("LASTNAME")
        private String lastName;

        @JsonProperty("SALES")
        private String sales;

        @JsonProperty("PRIMARYSALES")
        private String primarySales;

        @JsonProperty("RECRUITER")
        private String recruiter;

        @JsonProperty("PRIMARYRECRUITER")
        private String primaryRecruiter;

        @JsonProperty("Secondary Sales")
        private String secondarySales;

        @JsonProperty("Tertiary Sales")
        private String tertiarySales;

        @JsonProperty("Secondary Recruiter")
        private String secondaryRecruiter;

        @JsonProperty("Tertiary Recruiter")
        private String tertiaryRecruiter;

        @JsonProperty("DATELASTASSIGNED")
        private String dateLastAssigned;
    }

}
