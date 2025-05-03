package com.ask.ats.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * The type Entity association.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityAssociation {
    private Integer id;
    private String name;
    private String firstName;
    private String lastName;
}
