package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "GL_CHEQUE_RETURN_HDR")
public class ChequeReturn extends BaseEntity {

    @Id
    @AuditIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @AuditIgnore
    @Temporal(TemporalType.DATE)
    @Column(name = "TRANSACTION_DATE")
    private Date transactionDate;

    @Column(name = "GROUP_POID")
    @AuditIgnore
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    @AuditIgnore
    private Long companyPoid;

    @Column(name = "RECEIPT_NUMBER")
    private String receiptNumber;

    @Column(name = "DOC_REF")
    private String docRef;

    @Column(name = "CHQ_NUMBER")
    private String chequeNumber;

    @Column(name = "STATUS")
    private String status; // OPEN / RETURNED etc.

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "DELETED")
    private String deleted; // Y/N

    @Column(name = "CLOSE_DETAIL")
    private String closeDetail;

}
