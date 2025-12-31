package com.asg.finance.entity;

import com.asg.finance.entity.master.UnitMaster;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "GL_PETTY_CASH_ITEM_DTL")
@IdClass(GLPettyCashItemDtl.CompositeKey.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GLPettyCashItemDtl {
    @Id
    @Column(name = "TRANSACTION_POID")
    private Long transactionPoid;

    @Id
    @Column(name = "DET_ROW_ID")
    private Long detRowId;

    @ManyToOne
    @JoinColumn(name = "STOCK_POID", referencedColumnName = "STOCK_POID", insertable = false, updatable = false)
    private StockMasterEntity stockMaster;

    @ManyToOne
    @JoinColumn(name = "STOCK_UNIT_POID", referencedColumnName = "STOCK_UNIT_POID", insertable = false, updatable = false)
    private UnitMaster stockUnitMaster;

    @Column(name = "PO_QTY")
    private BigDecimal poQty;

    @Column(name = "DN_QTY")
    private BigDecimal dnQty;

    @Column(name = "QTY_RECEIVED")
    private BigDecimal qtyReceived;

    @Column(name = "PRICE")
    private BigDecimal price;

    @Column(name = "DISCOUNT")
    private BigDecimal discount;

    @Column(name = "TOTAL")
    private BigDecimal total;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "REF_DOC_ID", length = 100)
    private String refDocId;

    @Column(name = "REF_DOC_POID")
    private Long refDocPoid;

    @Column(name = "CHECK_ALL", length = 1)
    private String checkAll;

    @Column(name = "REF_DET_ROW_ID")
    private Long refDetRowId;

    @Column(name = "VAT_PARTY_NAME", length = 500)
    private String vatPartyName;

    @Column(name = "PARTY_INV_NUMBER", length = 500)
    private String partyInvNumber;

    @Column(name = "PARTY_INV_DATE")
    private LocalDate partyInvDate;

    @Column(name = "TAX_POID")
    private Long taxPoid;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeKey implements Serializable {
        private Long transactionPoid;
        private Long detRowId;
    }
}
