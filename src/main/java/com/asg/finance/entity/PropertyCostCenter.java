package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PROPERTY_COST_CENTER_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyCostCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROPERTY_COST_CENTER_POID")
    private Long propertyCostCenterPoid;

    @Column(name = "PROPERTY_COST_CENTER_CODE")
    private String propertyCostCenterCode;  // DB Trigger will generate this value

    @Column(name = "PROPERTY_COST_CENTER_NAME")
    private String propertyCostCenterName;

    @Column(name = "PROPERTY_DESCRIPTION")
    private String propertyDescription;

    @Column(name = "PROPERTY_TYPE")
    private String propertyType;

    @Column(name = "PARENT_PROPERTY_POID")
    private Long parentPropertyPoid;

    @Column(name = "COST_CENTER_POID")
    private Long costCenterPoid;

    @Column(name = "COMPANY_POID")
    private Long companyPoid;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "ACTIVE")
    private String active;

    @Column(name = "DELETED")
    private String deleted;

    @Column(name = "SEQNO")
    private Integer seqNo;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "LASTMODIFIED_BY")
    private String lastModifiedBy;

    @Column(name = "LASTMODIFIED_DATE")
    private LocalDateTime lastModifiedDate;
}
