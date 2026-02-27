package com.asg.finance.entity;

import com.asg.common.lib.annotation.AuditIgnore;
import com.asg.common.lib.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
@Entity
@Table(name = "GL_PAYING_TO_MASTER")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankPayee extends BaseEntity {
    private static final String YES_NO_FLAG_REGEX = "Y|N";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYING_POID", nullable = false)
    @AuditIgnore
    private Long payingPoid;

    @Column(name = "PAYING_NAME", nullable = false, length = 300)
    @NotBlank(message = "Paying name is mandatory")
    private String payingName;

    @Column(name = "PAYING_NAME2", length = 300)
    @Size(max = 300, message = "Secondary paying name cannot exceed 300 characters")
    private String payingName2;

    @Column(name = "PAY_GL_POID")
    @AuditIgnore
    private Long payGlPoid;

    @Column(name = "REMARKS", length = 500)
    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;

    @Column(name = "ACTIVE", length = 1)
    @Pattern(regexp = YES_NO_FLAG_REGEX, message = "Active must be 'Y' or 'N'")
    private String active;

    @Column(name = "DELETED")
    @Pattern(regexp = YES_NO_FLAG_REGEX, message = "Deleted flag must be 'Y' or 'N'")
    @AuditIgnore
    private String deleted;

    @Column(name = "SEQNO", precision = 5, scale = 0)
    @Digits(integer = 5, fraction = 0, message = "Sequence number can contain up to 5 whole digits")
    @Positive(message = "Sequence number must be greater than zero")
    private Integer seqNo;

    @PrePersist
    @PreUpdate
    private void validateState() {
        if (payingName == null || payingName.isBlank()) {
            throw new IllegalArgumentException("Payee name is mandatory");
        }

        if (seqNo != null && seqNo <= 0) {
            throw new IllegalArgumentException("Sequence number must be a positive integer");
        }

        if (active != null && !active.matches(YES_NO_FLAG_REGEX)) {
            throw new IllegalArgumentException("Active must be 'Y' or 'N'");
        }

        if (deleted != null && !deleted.matches(YES_NO_FLAG_REGEX)) {
            throw new IllegalArgumentException("Deleted flag must be 'Y' or 'N'");
        }
    }
}

