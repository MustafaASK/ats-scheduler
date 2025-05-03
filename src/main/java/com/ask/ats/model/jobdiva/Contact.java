package com.ask.ats.model.jobdiva;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Contact {

    @JsonAlias({"ID", "contactid"})
    private String contactid;

    @JsonAlias({"FIRSTNAME", "firstName"})
    private String firstname;

    @JsonAlias({"LASTNAME", "lastName"})
    private String lastname;

    @JsonAlias({"COMPANYID", "companyId"})
    private String companyId;

    @JsonAlias({"COMPANYNAME", "companyName"})
    private String companyName;

    @JsonAlias({"DEPARTMENTNAME", "departmentName"})
    private String department;

    @JsonAlias({"TITLE", "title"})
    private String title;

    @JsonAlias({"ACTIVE", "active"})
    private String active;

    @JsonAlias({"PRIMARY", "primaryStatus"})
    private String primaryStatus;

    @JsonAlias({"DATECREATED", "dateCreated"})
    private String dateCreated;

    @JsonAlias({"USERID", "userId"})
    private String userId;

    @JsonAlias({"DATEUPDATED", "dateUpdated"})
    private String dateUpdated;

    @JsonAlias({"DATEUSERFIELDUPDATED", "dateUserFieldUpdated"})
    private String dateUserFieldUpdated;

    @JsonAlias({"EMAIL", "email"})
    private String email;

    @JsonAlias({"ALTERNATE_EMAIL", "alternateEmail"})
    private String alternateemail;

    @JsonAlias({"PHONE1", "phone1"})
    private String phone1;

    @JsonAlias({"PHONE1_EXTENSION", "phone1Extension"})
    private String phone1Extension;

    @JsonAlias({"PHONE1_TYPE", "phone1Type"})
    private String phone1Type;

    @JsonAlias({"PHONE2", "phone2"})
    private String phone2;

    @JsonAlias({"PHONE2_EXTENSION", "phone2Extension"})
    private String phone2Extension;

    @JsonAlias({"PHONE2_TYPE", "phone2Type"})
    private String phone2Type;

    @JsonAlias({"PHONE3", "phone3"})
    private String phone3;

    @JsonAlias({"PHONE3_EXTENSION", "phone3Extension"})
    private String phone3Extension;

    @JsonAlias({"PHONE3_TYPE", "phone3Type"})
    private String phone3Type;

    @JsonAlias({"PHONE4", "phone4"})
    private String phone4;

    @JsonAlias({"PHONE4_EXTENSION", "phone4Extension"})
    private String phone4Extension;

    @JsonAlias({"PHONE4_TYPE", "phone4Type"})
    private String phone4Type;

    @JsonAlias({"ADDRESS1", "address1"})
    private String address1;

    @JsonAlias({"ADDRESS2", "address2"})
    private String address2;

    @JsonAlias({"CITY", "city"})
    private String city;

    @JsonAlias({"STATE", "state"})
    private String state;

    @JsonAlias({"ZIPCODE", "zipCode"})
    private String zipCode;

    @JsonAlias({"COUNTRYID", "countryId"})
    private String countryId;

    @JsonAlias({"SALES_PIPELINE", "salesPipeline"})
    private String salesPipeline;

    private List<Phone> phones;
    private List<Address> addresses;
    private String company;

    @Data
    @Builder
    public static class Phone {
        private Integer action;
        private String ext;
        private String type;
        private String phone;
    }

    @Data
    @Builder
    public static class Address {
        private Integer action;
        private String address1;
        private String address2;
        private String city;
        private String countryId;
        private boolean defaultAddress;
        private boolean deleted;
        private String freeText;
        private int id;
        private String state;
        private String zipCode;
    }
}
