package com.ask.ats.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Job order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobOrder {

    private Integer id;
    private Address address;
    private Long dateEnd;
    private BigDecimal clientBillRate;
    private ClientContact clientContact;
    private ClientCorporation clientCorporation;
    private Long dateAdded;
    private Long dateClosed;
    private Long dateLastModified;
    private String description;
    private String employmentType;
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private String externalID;
    private Boolean isDeleted;
    private Boolean isOpen;
    private Boolean isWorkFromHome;
    private Integer numOpenings;
    private String onSite;
    private BigDecimal payRate;
    private String publicDescription;
    private String reportTo;
    private ClientContact reportToClientContact;
    private ClientContact responseUser;
    private String source;
    private Long startDate;
    private String status;
    private String taxStatus;
    private String title;

}

