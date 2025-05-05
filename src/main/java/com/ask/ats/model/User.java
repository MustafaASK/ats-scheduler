package com.ask.ats.model;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    private Integer id;
    private Address address;
    private String companyName;
    private ClientCorporation corporation;
    private Long dateLastComment;
    private Long dateLastModified;
    private String email;
    private String email2;
    private String email3;
    private Boolean emailNotify;
    private String emailSignature;
    private Boolean enabled;
    private String externalEmail;
    private String firstName;
    private Boolean inboundEmailEnabled;
    private Boolean isDayLightSavingsTime;
    private Boolean isDeleted;
    private Boolean isHidden;
    private Boolean isLockedOut;
    private Boolean isOutboundFaxEnabled;
    private List<JobOrder> jobAssignments;
    private String lastName;
    private Boolean massMailOptOut;
    private String middleName;
    private String mobile;
    private String name;
    private String namePrefix;
    private String nameSuffix;
    private String nickName;
    private String occupation;
    private String pager;
    private String phone;
    private String phone2;
    private String phone3;
    private Boolean smsOptIn;
    private String status;
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private Integer timeZoneOffsetEST;
    private Long userDateAdded;
    private String username;
}
