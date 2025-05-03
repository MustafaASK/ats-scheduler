package com.ask.ats.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * The type To many entity association.
 *
 * @param <T> the type parameter
 */
@Data
@Builder
public class ToManyEntityAssociation<T> {
    private Integer total;
    private List<T> data;
}
