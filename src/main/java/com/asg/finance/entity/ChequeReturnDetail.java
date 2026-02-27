package com.asg.finance.entity;
import com.asg.common.lib.annotation.AuditIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "GL_CHEQUE_RETURN_DTL")
public class ChequeReturnDetail {

    @EmbeddedId
    @AuditIgnore
    private ChequeReturnDetailId id;


    @Column(name = "CHO_POID")
    @AuditIgnore
    private Long choPoid;

    @Temporal(TemporalType.DATE)
    @Column(name = "CHO_DATE")
    private Date choDate;

    @Column(name = "REF_DOC_ID", length = 20)
    private String refDocId;

    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "PYMT_TYPE", length = 10)
    @AuditIgnore
    private String pymtType;

    @Column(name = "CHQ_CARDNO", length = 50)
    private String chqCardNo;

    @Temporal(TemporalType.DATE)
    @Column(name = "CHQ_DATE")
    private Date chqDate;

    @Column(name = "BANK_POID")
    private Long bankPoid;

    @Column(name = "ADDRESS_POID")
    @AuditIgnore
    private Long addressPoid;

    @Column(name = "CHQ_AC_NAME", length = 100)
    private String chqAcName;

    @Column(name = "CHQ_AC_NO", length = 50)
    private String chqAcNo;

    @Column(name = "AMOUNT")
    private Double amount;

    @Column(name = "STATUS", length = 50)
    @AuditIgnore
    private String status;

    @Column(name = "REMARKS", length = 200)
    private String remarks;

    @Column(name = "VOUCHER_TYPE")
    @AuditIgnore
    private String voucherType;

    @Column(name = "PAYMENT_MAIN_POID")
    @AuditIgnore
    private Long paymentMainPoid;

    @Temporal(TemporalType.DATE)
    @Column(name = "RCP_DATE")
    private Date rcpDate;

    @Column(name = "REF_DOC_REF", length = 50)
    private String refDocRef;

    @Column(name = "CHO_DOC_ID", length = 20)
    @AuditIgnore
    private String choDocId;

    @Column(name = "CREATED_BY", length = 20)
    @AuditIgnore
    private String createdBy;

    @Column(name = "CREATED_DATE")
    @AuditIgnore
    private Date createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    @AuditIgnore
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    @AuditIgnore
    private Date lastModifiedDate;
}
