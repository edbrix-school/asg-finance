package com.asg.finance.dto.masters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Date;
import java.sql.Timestamp;

@Data
public class GlPostingDto {
    private Long id;
    private Date transactionDate;
    private String docRef;
    private String narration;
    private String companyCode;
    private String glAcType;
    private String glCode;
    private String glDescription;
    private Long drAmt;
    private Long crAmt;
    private String postedBy;
    private Timestamp postedDate;


}
