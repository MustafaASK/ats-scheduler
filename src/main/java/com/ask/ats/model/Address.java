package com.ask.ats.model;

import lombok.Builder;
import lombok.Data;

/**
 * The type Address.
 */
@Data
@Builder
public class Address {

    private String address1;
    private String address2;
    private String city;
    private String state;
    private String zip;
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private int countryID;
    private String countryCode;
    private String countryName;
    private String timezone;

}

