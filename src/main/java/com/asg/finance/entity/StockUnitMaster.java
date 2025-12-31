package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(
        name = "STOCK_UNIT_MASTER",
        uniqueConstraints = {
                @UniqueConstraint(name = "STOCK_UNIT_MASTER_UK1", columnNames = "STOCK_UNIT_CODE"),
                @UniqueConstraint(name = "STOCK_UNIT_MASTER_UK2", columnNames = "STOCK_UNIT_NAME")
        }
)
public class StockUnitMaster {

    @Id
    @Column(name = "STOCK_UNIT_POID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stock_unit_master_seq")
    @SequenceGenerator(
            name = "stock_unit_master_seq",
            sequenceName = "STOCK_UNIT_MASTER_SEQ",
            allocationSize = 1
    )
    private Long stockUnitPoid;

    @Column(name = "STOCK_UNIT_CODE", length = 20)
    private String stockUnitCode;

    @Column(name = "STOCK_UNIT_NAME", length = 100)
    private String stockUnitName;

    @Column(name = "STOCK_UNIT_NAME2", length = 100)
    private String stockUnitName2;

    @Column(name = "GROUP_POID")
    private Long groupPoid;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CLASSIFIED", length = 20)
    private String classified;
}
