package com.asg.finance.entity.master;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "STOCK_UNIT_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STOCK_UNIT_POID")
    private Long unitPoid;

    @Column(name = "STOCK_UNIT_CODE", length = 20, nullable = false, unique = true)
    private String unitCode;

    @Column(name = "STOCK_UNIT_NAME", length = 100)
    private String unitName;

    @Column(name = "STOCK_UNIT_NAME2", length = 100)
    private String unitName2;

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

    @Column(name = "SEQNO", precision = 5)
    private Integer seqNo;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CLASSIFIED", length = 20)
    private String classified;
}
