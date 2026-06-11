package com.asg.finance.entity;

import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "GL_BANK_MASTER_CHEQUE_DTL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(GlBankChequeDtlEntity.CompositeKey.class)
public class GlBankChequeDtlEntity extends BaseEntity {

    @Id
    @Column(name = "DET_ROW_ID", nullable = false)
    private Long detRowId;

    @Id
    @Column(name = "BANK_POID", nullable = false)
    private Long bankPoid;

    @Column(name = "CHQ_SIGN_TYPE", length = 20)
    private String chqSignType;

    @Column(name = "TOTAL_CHEQUES")
    private BigDecimal totalCheques;

    @Column(name = "START_CHQ_NO", length = 50)
    private String startChqNo;

    @Column(name = "END_CHQ_NO", length = 50)
    private String endChqNo;

    @Column(name = "REORDER_LEVEL")
    private BigDecimal reorderLevel;

    @Column(name = "CURRENT_CHQ_NO", length = 50)
    private String currentChqNo;

    @Column(name = "DEFAULT_PRINTER_ADDR", length = 200)
    private String defaultPrinterAddr;

    @Column(name = "DEFAULT_PRINTER_TRAY", length = 200)
    private String defaultPrinterTray;

    @Column(name = "STOCK_FINISHED_YN", length = 10)
    private String stockFinishedYn;

    @Column(name = "REMARKS", length = 100)
    private String remarks;

    @Column(name = "LAST_CHQ_NO", length = 50)
    private String lastChqNo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long detRowId;
        private Long bankPoid;
    }
}

