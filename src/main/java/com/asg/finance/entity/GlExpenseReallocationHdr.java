package com.asg.finance.entity;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "GL_EXPENSE_REALLOCATION_HDR")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlExpenseReallocationHdr {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "TRANSACTION_POID", nullable = false)
	private Long transactionPoid;

	@Column(name = "TRANSACTION_DATE", nullable = false)
	private Timestamp transactionDate;

	@Column(name = "GROUP_POID", nullable = false)
	private Long groupPoid;

	@Column(name = "COMPANY_POID", nullable = false)
	private Long companyPoid;

	@Column(name = "DOC_REF", length = 25)
	private String docRef;

	@Column(name = "NARRATION", length = 1000)
	private String narration;

	@Column(name = "EXPENSE_GROUP_GL", nullable = false)
	private Long expenseGroupGl;

	@Column(name = "FROM_COMPANY", nullable = false)
	private Long fromCompany;

	@Column(name = "JV_POID")
	private Long jvPoid;

	@Column(name = "JV_REF", length = 100)
	private String jvRef;

	@Column(name = "REMARKS", length = 1000)
	private String remarks;

	@Column(name = "COST_POID", length = 100)
	private String costPoid;

	@Column(name = "FROM_DATE")
	private Timestamp fromDate;

	@Column(name = "TO_DATE")
	private Timestamp toDate;

	@Column(name = "ALLOCATION_TYPE", length = 100)
	private String allocationType;

	@Column(name = "DELETED", length = 1)
	private String deleted;

	@Column(name = "REPORT_GENERATION", length = 1)
	private String reportGeneration;

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

}
