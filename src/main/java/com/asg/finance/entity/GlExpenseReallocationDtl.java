package com.asg.finance.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "GL_EXPENSE_REALLOCATION_DTL")
@IdClass(GlExpenseReallocationDtl.CompositeKey.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlExpenseReallocationDtl {

	@Id
	@Column(name = "TRANSACTION_POID")
	private Long transactionPoid;

	@Id
	@Column(name = "DET_ROW_ID")
	private Long detRowId;

	@Column(name = "COMPANY", nullable = false)
	private Long company;

	@Column(name = "COMPANY_NAME", length = 100)
	private String companyName;

	@Column(name = "SH", precision = 18, scale = 3)
	private BigDecimal sh;

	@Column(name = "FF", precision = 18, scale = 3)
	private BigDecimal ff;

	@Column(name = "FFS", precision = 18, scale = 3)
	private BigDecimal ffs;

	@Column(name = "FFP", precision = 18, scale = 3)
	private BigDecimal ffp;

	@Column(name = "PROPERTIES", precision = 18, scale = 3)
	private BigDecimal properties;

	@Column(name = "MTA", precision = 18, scale = 3)
	private BigDecimal mta;

	@Column(name = "PDA", precision = 18, scale = 3)
	private BigDecimal pda;

	@Column(name = "ADMIN", precision = 18, scale = 3)
	private BigDecimal admin;

	@Column(name = "TOTAL", precision = 18, scale = 3)
	private BigDecimal total;

	@Column(name = "REMARKS", length = 100)
	private String remarks;

	@Column(name = "CREATED_BY", length = 20, updatable = false)
	private String createdBy;

	@CreationTimestamp
	@Column(name = "CREATED_DATE", updatable = false)
	private Timestamp createdDate;

	@Column(name = "LASTMODIFIED_BY", length = 20)
	private String lastmodifiedBy;

	@UpdateTimestamp
	@Column(name = "LASTMODIFIED_DATE")
	private Timestamp lastmodifiedDate;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CompositeKey implements Serializable {
		private Long transactionPoid;
		private Long detRowId;
	}
}
