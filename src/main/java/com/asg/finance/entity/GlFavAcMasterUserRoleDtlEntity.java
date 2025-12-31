package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_FAV_AC_MASTER_USER_ROLE_DTL")
@Data
public class GlFavAcMasterUserRoleDtlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "FAV_AC_POID")
    private Long favAcPoid;

    @Column(name = "USER_ROLE_POID")
    private Long userRolePoid;

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
}
