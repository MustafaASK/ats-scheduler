package com.ask.ats.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;


/**
 * The type Validation result.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResult {

    private Boolean isValidJob;
    private Boolean isValidPool;
    private Map<String, String> missingFields;

}

