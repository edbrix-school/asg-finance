package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_FAV_AC_MASTER_GL_AC_DTL")
@Data
public class GlFavAcMasterGlAcDtlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "FAV_AC_POID")
    private Long favAcPoid;

    @Column(name = "GL_POID")
    private Long glPoid;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "SEQNO")
    private Long seqNo;

    @Column(name = "COMPANY")
    private Long company;

    @Column(name = "VIEW_CATEGORY")
    private String viewCategory;
}
