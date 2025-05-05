package com.ask.ats.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * The type Job submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobSubmission {

    private Integer id;
    private BigDecimal billRate;
    private Candidate candidate;
    private String comments;
    private Long dateAdded;
    private Long dateLastModified;
    private Long dateWebResponse;
    private Long endDate;
    private Boolean isDeleted;
    private Boolean isHidden;
    private JobOrder jobOrder;
    private ToManyEntityAssociation<User> owners;
    private BigDecimal payRate;
    private BigDecimal salary;
    private User sendingUser;
    private String source;
    private Long startDate;
    private String status;
    private Boolean isDoProcessManual;
}
