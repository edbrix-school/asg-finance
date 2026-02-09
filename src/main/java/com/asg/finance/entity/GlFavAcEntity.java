package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_FAV_AC_MASTER")
@Data
public class GlFavAcEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FAV_AC_POID")
    private Long favAcPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "FAV_AC_CODE")
    @AuditIgnore
    private String favAcCode;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DESCRIPTION2")
    private String description2;

    @Column(name = "ACTIVE")
    private String active = "Y";

    @Column(name = "SEQNO")
    private Long seqNo;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "DELETED")
    private String deleted = "N";
}
