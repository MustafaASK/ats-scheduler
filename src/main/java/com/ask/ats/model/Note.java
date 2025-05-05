package com.ask.ats.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

/**
 * The type Note.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Note {

    private Integer id;
    private String action;
    private String bhTimeStamp; // here bhTimeStamp coming as String when calling get api

    private ToManyEntityAssociation candidates;
    private ToManyEntityAssociation clientContacts;

    private EntityAssociation commentingPerson;
    private String comments;

    private ToManyEntityAssociation corporateUsers;

    private Timestamp dateAdded;
    private Timestamp dateLastModified;

    private ToManyEntityAssociation entities;

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private String externalID;
    private Boolean isDeleted;

    private EntityAssociation jobOrder;
    private ToManyEntityAssociation jobOrders;
    private ToManyEntityAssociation leads;

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private String linkedInID;

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    private String migrateGUID;

    private Integer minutesSpent;

    private ToManyEntityAssociation opportunities;
    private ToManyEntityAssociation people;

    private EntityAssociation personReference;

    private ToManyEntityAssociation placements;

    private List<String> primaryDepartmentName;

    private Timestamp truestDateAdded;

}
