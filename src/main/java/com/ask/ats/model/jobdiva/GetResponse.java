package com.ask.ats.model.jobdiva;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The type Get response.
 *
 * @param <T> the type parameter
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetResponse<T> {
    private String message;
    private List<T> data;
}
