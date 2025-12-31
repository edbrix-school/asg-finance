package com.asg.finance.entity.master;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;


@Data
public class GlPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TRANSACTION_DATE")
    private Date transactionDate;
    @Column(name = "DOC_REF")
    private String docRef;
    @Column(name = "NARRATION")
    private String narration;
    @Column(name = "COMPANY_CODE")
    private String companyCode;
    @Column(name = "GL_AC_TYPE")
    private String glAcType;
    @Column(name = "GL_CODE")
    private String glCode;
    @Column(name = "GL_DESCRIPTION")
    private String glDescription;
    @Column(name = "DR_AMT")
    private Long drAmt;
    @Column(name = "CR_AMT")
    private Long crAmt;
    @Column(name = "POSTED_BY")
    private String postedBy;
    @Column(name = "POSTED_DATE")
    private Timestamp postedDate;

}
