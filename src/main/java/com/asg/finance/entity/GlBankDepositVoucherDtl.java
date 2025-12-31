package com.asg.finance.entity;

import com.asg.finance.entity.key.GlBankDepositVoucherDtlKey;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "GL_BANK_DEPOSIT_VOUCHER_DTL")
@IdClass(GlBankDepositVoucherDtlKey.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankDepositVoucherDtl {
    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @Column(name = "BANK_POID")
    private Long bankPoid;

    @Column(name = "PYMT_TYPE", length = 50)
    private String pymtType;

    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "REF_DOC_REF", length = 100)
    private String refDocRef;

    @Column(name = "RCP_DATE")
    private LocalDate rcpDate;

    @Column(name = "CHQ_AC_NAME", length = 200)
    private String chqAcName;

    @Column(name = "CHQ_AC_NO", length = 50)
    private String chqAcNo;

    @Column(name = "CHQ_CARDNO", length = 50)
    private String chqCardNo;

    @Column(name = "CHQ_DATE")
    private LocalDate chqDate;

    @Column(name = "AMOUNT", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "SELECTED", length = 1)
    private String selected;

    @Column(name = "CHQ_SEQ_NUM")
    private Integer chqSeqNum;

    @Column(name = "PAYMENT_MAIN_POID")
    private Long paymentMainPoid;

    @Column(name = "REF_DOC_ID", length = 50)
    private String refDocId;
}
