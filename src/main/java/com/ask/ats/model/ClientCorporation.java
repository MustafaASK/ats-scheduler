package com.ask.ats.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Client corporation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientCorporation {

    private Integer id;
    private Address address;

    private BigDecimal annualRevenue;

    private Address billingAddress;

    private String billingContact;
    private String billingFrequency;
    private String billingPhone;


    private List<String> businessSectorList;


    private ToManyEntityAssociation childClientCorporations;
    // Note : Commented fields are Not supported in this release.
    //private ToManyEntityAssociation clientCorporationCertifications;

    private ToManyEntityAssociation clientContacts;


    private String companyDescription;

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private String companyURL;

    private String competitors;


    private String culture;

    private Timestamp customDate1;
    private Timestamp customDate2;
    private Timestamp customDate3;

    private Double customFloat1;
    private Double customFloat2;
    private Double customFloat3;

    private Integer customInt1;
    private Integer customInt2;
    private Integer customInt3;

    // Custom text fields
    private String customText1;
    private String customText2;
    private String customText3;
    private String customText4;
    private String customText5;


    private String customTextBlock1;
    private String customTextBlock2;
    private String customTextBlock3;

    private Timestamp dateAdded;
    private Timestamp dateFounded;
    private Timestamp dateLastModified;

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private String externalID;

    private Double feeArrangement;


    private String funding;

    private List<String> industryList;

    private String invoiceFormat;

    private ToManyEntityAssociation leads;
    private String name;

    private String notes;

    private Integer numEmployees;
    private Integer numOffices;


    private ToManyEntityAssociation owners;

    private String ownership;

    private EntityAssociation parentClientCorporation;

    private String phone;


    //private ToManyEntityAssociation rateCards;

    private String revenue;

    private ToManyEntityAssociation requirements;
    private String status;

    private Double taxRate;

    private String tickerSymbol;

    private Integer workWeekStart;


}
