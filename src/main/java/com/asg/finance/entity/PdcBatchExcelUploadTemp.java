package com.asg.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "PDC_BATCH_EXCEL_UPLOAD_TEMP")
public class PdcBatchExcelUploadTemp {

    @Id
    @Column(name = "CHEQUE_NUMBER")
    private String chequeNumber;

    @Column(name = "SN", length = 100)
    private String sn;

    @Column(name = "CHEQUE_DATE")
    private String chequeDate;

    @Column(name = "CHEQUE_AMOUNT")
    private String chequeAmount;

    @Column(name = "DR_AMT")
    private String drAmt;

    @Column(name = "DR_AMT2")
    private String drAmt2;
}
