package com.ask.ats.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/**
 * The type Candidate.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Candidate {

    private Integer id;
    private Address address;
    private Integer clientRating;
    private String companyName;
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private String companyURL;
    private Integer customInt1;
    private Long dateAdded;
    private Long dateAvailable;
    private Long dateAvailableEnd;
    private Long dateI9Expiration;
    private Long dateLastComment;
    private Long dateLastModified;
    private Long dateNextCall;
    private Long dateOfBirth;
    private String degreeList;
    private String description;
    private String educationDegree;
    private String email;
    private String employmentPreference;
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private String externalID;
    private String firstName;
    private BigDecimal hourlyRate;
    private Boolean isDeleted;
    private String lastName;
    private String mobile;
    private String name;
    private String occupation;
    private String phone;
    private String preferredContact;
    private String source;
    private String status;
    private String type;
    private Boolean willRelocate;
    private Boolean workAuthorized;
    private Boolean massMailOptOut;
    private Boolean smsOptIn;

}

