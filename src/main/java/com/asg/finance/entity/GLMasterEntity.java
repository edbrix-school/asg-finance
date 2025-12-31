package com.asg.finance.entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Entity
@Table(name = "GL_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class GLMasterEntity {

    @Id
    @Column(name = "GL_POID")
    @GeneratedValue(strategy = GenerationType.IDENTITY) // or appropriate strategy
    private Long glPoid;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "GL_CODE", unique = true)
    private String glCode;

    @Column(name = "GL_DESCRIPTION")
    private String description;

    @Column(name = "GL_DESCRIPTION2")
    private String description2;

    @Column(name = "GL_TYPE")
    private String type;

    @Column(name = "GROUP_GL_POID")
    private Long groupGlPoid;  // i.e. parent

    @Column(name = "GL_AC_TYPE")
    private String accountType;

    @Column(name = "CONTROL_AC_NATURE")
    private String controlAcType;

    @Column(name = "COST_GROUP")
    private String costGroup;

    @Column(name = "INTER_COMPANY_AC")
    private String interCompanyFlag;  // 'Y' / 'N'

    @Column(name = "INTER_COMPANY_POID")
    private Long interCompanyId;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "ACTIVE")
    private String activeFlag;  // 'Y' / 'N'

    @Column(name = "PREPAYMENT_LEDGER")
    private String prepaymentLedgerFlag; // 'Y' / 'N'

    @Column(name = "BILLWISE")
    private String billWiseFlag;

    @Column(name = "DELETED", length = 1)
    private String deletedFlag = "N";  // default 'N'

    // KeyFavorite is not in DB — maybe in a separate flag or virtual; include if needed
    // Payment detail: one-to-many relationship with GL_MASTER_PYMT_DTL
    // Multiple payment details can exist for a single GL Master
    @OneToMany(mappedBy = "glMaster", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private java.util.List<GLPaymentDetailsEntity> paymentDetails = new ArrayList<>();

    // Company DTLs
    @OneToMany(mappedBy = "glMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<GLMasterCompanyDtlEntity> companyDetails = new ArrayList<>();

    // audit fields
    @Column(name = "CREATED_BY")
    private String createdBy;
    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;
    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;
    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    // Additional fields from the original GLMaster
    @Column(name = "GROUP_CODE_OLD", length = 20)
    private String groupCodeOld;

    @Column(name = "AMOUNT_LIMIT")
    private Double amountLimit;

    @Column(name = "AMOUNT_ROL")
    private Double amountRol;

    @Column(name = "OLD_ORGCODE", length = 50)
    private String oldOrgCode;

    @Column(name = "OLD_ORIGINAL_CODE", length = 20)
    private String oldOriginalCode;

    @Column(name = "OLD_MOD_CODE", length = 20)
    private String oldModCode;

    // Helper methods for boolean conversions

    public Boolean isDeleted() {
        return "Y".equals(deletedFlag);
    }

    // Helper method to determine if this has children (would need to be checked via repository)
    public Boolean hasChildren() {
        // This would typically be determined by checking if any records have this GL_POID as their GROUP_GL_POID
        // For now, we'll use GL_TYPE as a heuristic
        return "MAIN_GROUP".equals(type) || "SUB_GROUP".equals(type);
    }
}
