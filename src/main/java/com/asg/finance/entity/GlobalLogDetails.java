package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Table(name = "GLOBAL_LOG_DETAILS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalLogDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOG_POID")
    private Long logPoid;

    @Column(name = "LOG_USER_POID")
    private Long logUserPoid;

    @Column(name = "LOG_DATETIME")
    private Timestamp logDateTime;

    @Column(name = "LOG_DOC_ID", length = 20)
    private String logDocId;

    @Column(name = "LOG_DOC_KEY_POID", length = 20)
    private String logDocKeyPoid;

    @Column(name = "FIELD_NAME", length = 100)
    private String fieldName;

    @Column(name = "OLD_VALUE", length = 4000)
    private String oldValue;

    @Column(name = "NEW_VALUE", length = 4000)
    private String newValue;

    @Column(name = "LOG_DETAILS", length = 200)
    private String logDetails;

    @Column(name = "LOG_TABLE", length = 100)
    private String logTable;
}