package com.ask.ats.model.agileone;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The type Job details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDetails {

    @JsonProperty("jobID")
    private int jobId;

    @JsonProperty("jobTitle")
    private String jobTitle;

    @JsonProperty("jobStatus")
    private String jobStatus;

    @JsonProperty("jobType")
    private String jobType;

    @JsonProperty("numberOfPosition")
    private int numberOfPosition;

    @JsonProperty("numberOfOpenPosition")
    private Integer numberOfOpenPosition;

    @JsonProperty("jobCategory")
    private String jobCategory;

    @JsonProperty("payType")
    private String payType;

    @JsonProperty("startDate")
    private String startDate;

    @JsonProperty("startTime")
    private String startTime;

    @JsonProperty("endDate")
    private String endDate;

    @JsonProperty("endTime")
    private String endTime;

    @JsonProperty("lunchBreak")
    private int lunchBreak;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("minPayRate")
    private double minPayRate;

    @JsonProperty("maxPayRate")
    private double maxPayRate;

    @JsonProperty("minBillRate")
    private double minBillRate;

    @JsonProperty("maxBillRate")
    private double maxBillRate;

    @JsonProperty("estimatedCost")
    private double estimatedCost;

    @JsonProperty("description")
    private String description;

    @JsonProperty("responsibility")
    private String responsibility;

    @JsonProperty("workAddress1")
    private String workAddress1;

    @JsonProperty("workAddress2")
    private String workAddress2;

    @JsonProperty("workCity")
    private String workCity;

    @JsonProperty("workState")
    private String workState;

    @JsonProperty("workPostalCode")
    private String workPostalCode;

    @JsonProperty("workCountryISOCode")
    private String workCountryISOCode;

    @JsonProperty("onHold")
    private boolean onHold;

    @JsonProperty("clientLocationName")
    private String clientLocationName;

    @JsonProperty("clientName")
    private String clientName;

    @JsonProperty("createdDate")
    private String createdDate;

    @JsonProperty("lastActivityDate")
    private String lastActivityDate;

    @JsonProperty("requestorFullName")
    private String requestorFullName;

    @JsonProperty("reportToFullName")
    private String reportToFullName;

    @JsonProperty("workDay")
    private WorkDay workDay;

    @JsonProperty("skill")
    private List<Skill> skill;

    @JsonProperty("education")
    private List<Education> education;

    @JsonProperty("specialBill")
    private SpecialBill specialBill;

    @JsonProperty("screening")
    private List<Screening> screening;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkDay {
        @JsonProperty("sunday")
        private boolean sunday;

        @JsonProperty("monday")
        private boolean monday;

        @JsonProperty("tuesday")
        private boolean tuesday;

        @JsonProperty("wednesday")
        private boolean wednesday;

        @JsonProperty("thursday")
        private boolean thursday;

        @JsonProperty("friday")
        private boolean friday;

        @JsonProperty("saturday")
        private boolean saturday;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Skill {
        @JsonProperty("code")
        private String code;

        @JsonProperty("description")
        private String description;

        @JsonProperty("experience")
        private int experience;

        @JsonProperty("comments")
        private String comments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Education {
        @JsonProperty("degreeCode")
        private String degreeCode;

        @JsonProperty("degreeDescription")
        private String degreeDescription;

        @JsonProperty("major")
        private String major;

        @JsonProperty("required")
        private boolean required;

        @JsonProperty("prefered")
        private boolean prefered;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecialBill {
        @JsonProperty("comp")
        private String comp;

        @JsonProperty("costr")
        private String costr;

        @JsonProperty("glacc")
        private String glacc;

        @JsonProperty("ft")
        private String ft;

        @JsonProperty("prodid")
        private String prodid;

        @JsonProperty("jcatty")
        private String jcatty;

        @JsonProperty("rsnrq")
        private String rsnrq;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Screening {
        @JsonProperty("code")
        private String code;

        @JsonProperty("required")
        private boolean required;

        @JsonProperty("description")
        private String description;
    }
}