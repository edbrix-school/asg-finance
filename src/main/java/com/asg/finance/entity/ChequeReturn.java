package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "GL_CHEQUE_RETURN_HDR")
public class ChequeReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Temporal(TemporalType.DATE)
    @Column(name = "TRANSACTION_DATE")
    private Date transactionDate;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

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

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_DATE")
    private Date createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LASTMODIFIED_DATE")
    private Date lastModifiedDate;

    @Column(name = "CLOSE_DETAIL")
    private String closeDetail;

    @OneToMany(mappedBy = "chequeReturn", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ChequeReturnDetail> chequeDetails;

    @OneToMany(mappedBy = "chequeReturn", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ChequeReturnGlDetail> glDetails;
}
