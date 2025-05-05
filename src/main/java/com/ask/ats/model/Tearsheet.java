package com.ask.ats.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Tearsheet.
 */
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tearsheet {

    private Integer id;
    private ToManyEntityAssociation<Candidate> candidates;

    private ToManyEntityAssociation<ClientContact> clientContacts;

    private Long dateAdded;

    private Long dateLastModified;

    private String description;

    private Boolean isDeleted;

    private Boolean isPrivate;

    private ToManyEntityAssociation<JobOrder> jobOrders;

    private String name;

    private User owner;

    private ToManyEntityAssociation recipients;

    private Boolean isDoProcessManual;
}