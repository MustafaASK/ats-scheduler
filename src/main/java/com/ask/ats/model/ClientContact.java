package com.ask.ats.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Client contact.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientContact {

    private Integer id;

    private Address address;


    private ToManyEntityAssociation businessSectors;

    private EntityAssociation category;
    private ToManyEntityAssociation categories;

    private String certifications;

    private EntityAssociation clientCorporation;

    private String comments;

    private LocalDateTime customDate1;
    private LocalDateTime customDate2;
    private LocalDateTime customDate3;

    private Double customFloat1;
    private Double customFloat2;
    private Double customFloat3;

    private Integer customInt1;
    private Integer customInt2;
    private Integer customInt3;


    // private List<Integer> customObject1;

    private String customText1;
    private String customText2;
    private String customText3;
    private String customText4;
    private String customText5;
    private String customText6;
    private String customText7;
    private String customText8;
    private String customText9;
    private String customText10;
    private String customText11;
    private String customText12;
    private String customText13;
    private String customText14;
    private String customText15;
    private String customText16;
    private String customText17;
    private String customText18;
    private String customText19;
    private String customText20;

    private String customTextBlock1;
    private String customTextBlock2;
    private String customTextBlock3;
    private String customTextBlock4;
    private String customTextBlock5;

    private Timestamp dateAdded;  // Note : dateAdded and dateLastModified are coming as Long from bullhorn
    private Timestamp dateLastModified;
    private LocalDateTime dateLastVisit;

    private String description;

    private String desiredCategories;
    private String desiredSkills;
    private String desiredSpecialties;

    private String division;

    private String email; // Primary (work) email address
    private String email2; // Additional email address (personal)
    private String email3; // Another additional email address
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private String externalID; // External identifier for record migrations

    private String fax; // Primary work fax number
    private String fax2; // Additional fax number (personal)
    private String fax3; // Another additional fax number

    private String firstName; // Contact's first name
    private Boolean isAnonymized; // Anonymized status of the record
    private Boolean isDayLightSavings; // Daylight Saving Time status for location
    private Boolean isDeleted; // Deleted status in Bullhorn system
    private String lastName; // Contact's last name

    private ToManyEntityAssociation leads;

    // private Integer linkedPerson; // Linked person (Candidate/ClientContact)

    private Boolean massMailOptOut; // Mass email opt-out status

    private String middleName; // Middle name of the contact

    private String mobile; // Mobile phone number
    private String name; // Full name (combination of first and last name)
    private String namePrefix; // Name prefix (e.g., Dr., Mr.)
    private String nameSuffix; // Name suffix (e.g., Jr.)
    private String nickName; // Nickname
    private Integer numEmployees; // Number of employees reporting to contact

    private String occupation; // Job title
    private String office; // Office location for multi-location companies

    private EntityAssociation owner; // Primary owner of this contact record

    private String pager; // Pager number
    private String password; // Password for Bullhorn login

    private String phone;
    private String phone2; // Home phone number
    private String phone3; // Another phone number

    private String preferredContact;

    private Integer referredByPerson;
    private Integer reportToPerson;

    private Address secondaryAddress;

    private ToManyEntityAssociation secondaryOwners;

    private ToManyEntityAssociation skills;
    private Boolean smsOptIn;  // Note : not allowed on update operation
    private String source;
    private String status;

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private Integer timeZoneOffsetEST;

    private String type;
    private String username;

}

