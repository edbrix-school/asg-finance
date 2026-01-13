package com.asg.finance.entity;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "GL_BANK_RECONCILIATION_TABLE")
@Data
public class BankReconciliation {

	@Column(name = "TRANSACTION_GROUP_POID")
	private Long transactionGroupPoid;

	@Column(name = "DET_ROW_ID")
	private Long detRowId;

	@Column(name = "TRANSACTION_COMPANY_POID")
	private Long transactionCompanyPoid;

	@Column(name = "DOC_ID")
	private String docId;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "TRANSACTION_POID")
	private Long transactionPoid;

	@Column(name = "TRANSACTION_DATE")
	private Date transactionDate;

	@Column(name = "DOC_REF")
	private String docRef;

	@Column(name = "CHEQUE_REF")
	private String chequeRef;

	@Column(name = "NARRATION")
	private String narration;

	@Column(name = "GL_COMPANY_POID")
	private Long glCompanyPoid;

	@Column(name = "GL_POID")
	private Long glPoid;

	@Column(name = "DR_AMT")
	private BigDecimal drAmt;

	@Column(name = "CR_AMT")
	private BigDecimal crAmt;

	@Column(name = "POSTED_BY")
	private Long postedBy;

	@Column(name = "POSTED_DATE")
	private Date postedDate;

	@Column(name = "CLEARANCE_DATE")
	private Date clearanceDate;

	@Column(name = "LAST_POSTED_BY")
	private Long lastPostedBy;

	@Column(name = "LAST_POSTED_DATE")
	private Date lastPostedDate;

	@Column(name = "OLD_REF_NO")
	private String oldRefNo;

	@Column(name = "RECONCILE_TYPE")
	private String reconcileType;

}
