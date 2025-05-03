package com.ask.ats.model;

import java.sql.Timestamp;
import lombok.Builder;
import lombok.Data;

/**
 * The type Applicant.
 */
@Data
@Builder
public class Applicant {

    private Integer applicantId;
    private Integer jobId;
    private Integer userId;
    private Integer clientId;
    private String source;
    private Integer status;
    private Integer score;
    private Timestamp saveDate;
    private Boolean isDelete;
    private Integer openId;

}
