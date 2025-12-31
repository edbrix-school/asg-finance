package com.asg.finance.entity;

import com.asg.finance.entity.key.GlBankDebitItemDtlId;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "GL_BANK_DEBIT_ITEM_DTL",
        uniqueConstraints = {
                @UniqueConstraint(name = "GL_BANK_DEBIT_ITEM_D_PK", columnNames = {"TRANSACTION_POID", "DET_ROW_ID"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlBankDebitItemDtl {

    @EmbeddedId
    private GlBankDebitItemDtlId id;

    @Column(name = "STOCK_POID")
    private Long stockPoid;

    @Column(name = "STOCK_UNIT_POID")
    private Long stockUnitPoid;

    @Column(name = "PO_QTY", precision = 15, scale = 2)
    private BigDecimal poQty;

    @Column(name = "DN_QTY", precision = 15, scale = 2)
    private BigDecimal dnQty;

    @Column(name = "QTY_RECEIVED", precision = 15, scale = 2)
    private BigDecimal qtyReceived;

    @Column(name = "PRICE", precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "DISCOUNT", precision = 15, scale = 2)
    private BigDecimal discount;

    @Column(name = "TOTAL", precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "REMARKS", length = 100)
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

    @Column(name = "REF_DET_ROW_ID")
    private Long refDetRowId;
}
