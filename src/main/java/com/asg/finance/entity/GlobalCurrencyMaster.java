package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "GLOBAL_CURRENCY_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class GlobalCurrencyMaster {

    @Id
    @Column(name = "CURRENCY_POID")
    private Long currencyPoid;

    @Column(name = "GROUP_POID", nullable = false)
    private Long groupPoid;

    @Column(name = "CURRENCY_CODE", length = 20, nullable = false, unique = true)
    private String currencyCode;

    @Column(name = "CURRENCY_NAME", length = 100, nullable = false, unique = true)
    private String currencyName;

    @Column(name = "CURRENCY_NAME2", length = 100)
    private String currencyName2;

    @Column(name = "CURRENCY_DECIMALS")
    private Integer currencyDecimals;

    @Column(name = "ACTIVE", length = 1)
    private String active;

    @Column(name = "SEQNO")
    private Integer seqno;

    @Column(name = "CURRENCY_SHORT_NAME", length = 20)
    private String currencyShortName;

    @Column(name = "COIN_SHORT_NAME", length = 20)
    private String coinShortName;

    @Column(name = "NUMBER_FORMAT_CURRENCY", length = 100)
    private String numberFormatCurrency;

    @Column(name = "DELETED", length = 1)
    private String deleted;

    @Column(name = "CREATED_BY", length = 20)
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY", length = 20)
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    public Boolean isActive() {
        return "Y".equals(active);
    }

    public Boolean isDeleted() {
        return "Y".equals(deleted);
    }
}