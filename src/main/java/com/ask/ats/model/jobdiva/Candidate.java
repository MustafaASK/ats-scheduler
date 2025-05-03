package com.ask.ats.model.jobdiva;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


/**
 * The type Candidate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Candidate {

    @JsonAlias({"ID", "CANDIDATEID", "candidateid"})
    private String candidateid;

    @JsonAlias({"FIRSTNAME", "firstName"})
    private String firstName;

    @JsonAlias({"LASTNAME", "lastName"})
    private String lastName;

    @JsonAlias({"STATE", "state"})
    private String state;

    @JsonAlias({"CITY", "city"})
    private String city;

    @JsonAlias({"COUNTRY", "country"})
    private String country;

    @JsonAlias({"ABSTRACT", "abstractText"})
    private String abstractText;

    @JsonAlias({"PHONE", "phone"})
    private String phone;

    @JsonAlias({"RECEIVED", "received"})
    private String received;

    @JsonAlias({"AVAILABLE", "available"})
    private String available;

    @JsonAlias({"LASTNOTE", "lastNote"})
    private String lastNote;

    @JsonAlias({"ADDRESS1", "address1"})
    private String address1;

    @JsonAlias({"ADDRESS2", "address2"})
    private String address2;

    @JsonAlias({"ZIPCODE", "zipCode"})
    private String zipCode;

    @JsonAlias({"WORKPHONE", "workPhone"})
    private String workPhone;

    @JsonAlias({"WORKPHONE_EXT", "workPhoneExt"})
    private String workPhoneExt;

    @JsonAlias({"HOMEPHONE", "homePhone"})
    private String homePhone;

    @JsonAlias({"CELLPHONE", "cellPhone"})
    private String cellPhone;

    @JsonAlias({"FAX", "fax"})
    private String fax;

    @JsonAlias({"PHONE1", "phone1"})
    private String phone1;

    @JsonAlias({"PHONE1_EXT", "phone1Ext"})
    private String phone1Ext;

    @JsonAlias({"PHONE1_TYPE", "phone1Type"})
    private String phone1Type;

    @JsonAlias({"PHONE2", "phone2"})
    private String phone2;

    @JsonAlias({"PHONE2_EXT", "phone2Ext"})
    private String phone2Ext;

    @JsonAlias({"PHONE2_TYPE", "phone2Type"})
    private String phone2Type;

    @JsonAlias({"PHONE3", "phone3"})
    private String phone3;

    @JsonAlias({"PHONE3_EXT", "phone3Ext"})
    private String phone3Ext;

    @JsonAlias({"PHONE3_TYPE", "phone3Type"})
    private String phone3Type;

    @JsonAlias({"PHONE4", "phone4"})
    private String phone4;

    @JsonAlias({"PHONE4_EXT", "phone4Ext"})
    private String phone4Ext;

    @JsonAlias({"PHONE4_TYPE", "phone4Type"})
    private String phone4Type;

    @JsonAlias({"EMAIL", "email"})
    private String email;

    @JsonAlias({"ALTERNATEEMAIL", "alternateEmail"})
    private String alternateEmail;

    @JsonAlias({"DATECREATED", "dateCreated"})
    private String dateCreated;

    @JsonAlias({"DATEUPDATED", "dateUpdated"})
    private String dateUpdated;

    @JsonAlias({"DATEUSERFIELDUPDATED", "dateUserFieldUpdated"})
    private String dateUserFieldUpdated;

    @JsonAlias({"DATEPROFILEUPDATED", "dateProfileUpdated"})
    private String dateProfileUpdated;

    @JsonAlias({"CURRENTSALARY", "currentSalary"})
    private String currentSalary;

    @JsonAlias({"CURRENTSALARYPER", "currentSalaryPer"})
    private String currentSalaryPer;

    @JsonAlias({"PREFERREDSALARYMIN", "preferredSalaryMin"})
    private String preferredSalaryMin;

    @JsonAlias({"PREFERREDSALARYPER", "preferredSalaryPer"})
    private String preferredSalaryPer;

    @JsonAlias({"RESUMECOUNT", "resumeCount"})
    private String resumeCount;

    @JsonAlias({"LINKEDIN", "linkedIn"})
    private String linkedIn;

    @JsonAlias({"MYSPACE", "mySpace"})
    private String mySpace;

    @JsonAlias({"FACEBOOK", "facebook"})
    private String facebook;

    @JsonAlias({"TWITTER", "twitter"})
    private String twitter;

    @JsonAlias({"YOUTUBE", "youtube"})
    private String youtube;

    @JsonAlias({"STACKOVERFLOW", "stackOverflow"})
    private String stackOverflow;

    @JsonAlias({"INSTAGRAM", "instagram"})
    private String instagram;

    @JsonAlias({"GITHUB", "github"})
    private String github;

    @JsonAlias({"PROFESSIONAL_WEBSITE", "professionalWebsite"})
    private String professionalWebsite;

    @JsonAlias({"COUNTRY_ID", "countryId"})
    private String countryId;

    @JsonAlias({"CURRENT_SALARY_UNIT", "currentSalaryUnit"})
    private String currentSalaryUnit;

    @JsonAlias({"EDUCATIONS", "educations"})
    private List<CandidateEducation> educations;

    @JsonAlias({"END_DATE", "endDate"})
    private LocalDateTime endDate;

    @JsonAlias({"NARRATIVE", "narrative"})
    private String narrative;

    @JsonAlias({"OWNER", "owner"})
    private Long owner;

    @JsonAlias({"PREFERRED_SALARY", "preferredSalary"})
    private Double preferredSalary;

    @JsonAlias({"PREFERRED_SALARY_UNIT", "preferredSalaryUnit"})
    private String preferredSalaryUnit;

    @JsonAlias({"PROFESSIONS_SPECIALTIES", "professionsSpecialties"})
    private CandidateProfSpec professionsSpecialties;

    @JsonAlias({"RESUME_SOURCE", "resumeSource"})
    private Integer resumeSource;

    @JsonAlias({"START_DATE", "startDate"})
    private String startDate;

    @JsonAlias({"TITLE_SKILL_CERTIFICATION", "titleSkillCertification"})
    private String titleSkillCertification;

    @JsonAlias({"TITLE_SKILL_CERTIFICATIONS", "titleSkillCertifications"})
    private List<TitleSkillCertification> titleSkillCertifications;

    @JsonAlias({"YEARS", "years"})
    private Integer years;

    private String resumeId;

    @Data
    @Builder
    public static class CandidateEducation {

        @JsonAlias({"DEGREE", "degree"})
        private String degree;

        @JsonAlias({"MAJOR", "major"})
        private String major;

        @JsonAlias({"SCHOOL", "school"})
        private String school;

        @JsonAlias({"YEAR", "year"})
        private Integer year;
    }

    /**
     * The type Candidate prof spec.
     */
    @Data
    @Builder
    public static class CandidateProfSpec {

        @JsonAlias({"PROFESSIONS", "professions"})
        private Map<String, List<Object>> professions;
    }

    /**
     * The type Title skill certification.
     */
    @Data
    @Builder
    public static class TitleSkillCertification {

        @JsonAlias({"END_DATE", "endDate"})
        private LocalDateTime endDate;

        @JsonAlias({"START_DATE", "startDate"})
        private LocalDateTime startDate;

        @JsonAlias({"TITLE_SKILL_CERTIFICATION", "titleSkillCertification"})
        private String titleSkillCertification;

        @JsonAlias({"YEARS", "years"})
        private Integer years;
    }
}
