package com.ask.ats.model.jobdiva;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CandidateNotesListResponse {
    private String message;
    private Map<String, List<CandidateNote>> data;
} 