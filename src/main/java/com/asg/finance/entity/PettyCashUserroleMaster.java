package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "GL_PETTY_CASH_USERROLE_MASTER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PettyCashUserroleMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REF_TYPE_POID", nullable = false)
    @AuditIgnore
    private Long refTypePoid;

    @Column(name = "REF_TYPE", length = 20)
    private String refType;

    @Column(name = "DESCRIPTION", length = 100)
    private String description;

    @Column(name = "USER_ROLE_POID", length = 100)
    private String userRolePoid;

    @Column(name = "GL_POID", length = 100)
    private String glPoid;

    @Column(name = "VALID_UNTIL")
    @AuditIgnore
    private LocalDate validUntil;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "DELETED", length = 1)
    @AuditIgnore
    private String deleted = "N";

    @Column(name = "SEQNO", precision = 5)
    private Integer seqNo;

    @Column(name = "CREATED_BY", length = 20)
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private Timestamp createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private LocalDateTime lastModifiedDate;

}
