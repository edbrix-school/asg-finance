package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "GLOBAL_TERMS_CUSTOM_CHANGES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalTermsCustomChanges {
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GlobalTermsCustomChangesId implements Serializable {
        @Column(name = "DOC_ID", nullable = false, length = 25)
        private String docId;

        @Column(name = "DOC_KEY_POID", nullable = false)
        private Long docKeyPoid;

        @Column(name = "COMPANY_POID", nullable = false)
        private Long companyPoid;

        @Column(name = "DET_ROW_ID", nullable = false)
        private Long detRowId;

    }

    @EmbeddedId
    private GlobalTermsCustomChangesId id;

    @Column(name = "REF_TERMS_POID")
    private Long refTermsPoid;

    @Column(name = "CLAUSE_NO", length = 100)
    private String clauseNo;

    @Column(name = "CLAUSE_DETAILS", length = 2000)
    private String clauseDetails;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    }
