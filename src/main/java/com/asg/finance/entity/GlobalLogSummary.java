package com.asg.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "GLOBAL_LOG_SUMMARY")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalLogSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOG_POID")
    private Long logPoid;

    @Column(name = "LOG_USER_POID")
    private Long logUserPoid;

    @Column(name = "LOG_DATETIME")
    private LocalDateTime logDateTime;

    @Column(name = "LOG_DETAILS", length = 2000)
    private String logDetails;

    @Column(name = "LOG_DOC_ID", length = 20)
    private String logDocId;

    @Column(name = "LOG_DOC_KEY_POID", length = 20)
    private String logDocKeyPoid;
}