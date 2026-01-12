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
@Table(name = "GL_EXPENSE_REALLOCATION_XL_DTL")
@IdClass(GlExpenseReallocationXlDtl.CompositeKey.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlExpenseReallocationXlDtl {
	@Id
	@Column(name = "TRANSACTION_POID")
	private Long transactionPoid;

	@Id
	@Column(name = "DET_ROW_ID")
	private Long detRowId;

	@Column(name = "COMPANY")
	private Long company;

	@Column(name = "COMPANY_CODE", length = 100)
	private String companyCode;

	@Column(name = "COST_CENTRE", length = 100)
	private String costCentre;

	@Column(name = "PERCENT", precision = 18, scale = 3)
	private BigDecimal percent;

	@Column(name = "REMARKS", length = 100)
	private String remarks;

	@Column(name = "CREATED_BY", length = 20)
	private String createdBy;

	@CreationTimestamp
	@Column(name = "CREATED_DATE")
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
