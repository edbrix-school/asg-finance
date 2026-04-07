package com.asg.finance.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;

import com.asg.finance.dto.ChequeStockResponse;
import com.asg.finance.dto.PendingChequeResponse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;

@Repository
@Transactional(readOnly = true)
public class ChequePrintingRepositoryImpl implements ChequePrintingRepository {

	@PersistenceContext
	private EntityManager em;

	@Override
	@SuppressWarnings("unchecked")
	public List<PendingChequeResponse> fetchPendingCheques() {
		StoredProcedureQuery sp = em.createStoredProcedureQuery("PROC_BANK_PENDING_CHEQUE_VIEW");
		sp.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);
		sp.execute();
		List<Object[]> rows = sp.getResultList();
		List<PendingChequeResponse> out = new ArrayList<>();
		for (Object[] r : rows) {
			PendingChequeResponse dto = new PendingChequeResponse();
			dto.setTransactionPoid(getLong(r, 0));
			dto.setCompany(getString(r, 1));
			dto.setCompanyPoid(getLong(r, 2));
			dto.setBankPoid(getLong(r, 3));
			dto.setPartyName(getString(r, 4));
			dto.setBank(getString(r, 5));
			dto.setPvNo(getString(r, 6));
			dto.setDocDate(getDate(r, 7));
			dto.setChqDate(getDate(r, 8));
			dto.setChequeAmount(getBigDecimal(r, 9));
			dto.setChqSignType(getString(r, 10));
			dto.setChqCardNo(getString(r, 11));
			dto.setAccountPayee(getString(r, 12));
			out.add(dto);
		}
		return out;
	}

	@Override
	public Map<String, String> validateBeforeChequePrint(Long groupPoid, Long loginUserPoid, String companyPoid, Long bankPoid,
			String chqSignType, Long transactionPoid, String suppressBalanceCheck) {
		StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_BANK_BEFORE_CHEQ_PRINT");

		query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(2, Long.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(5, String.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(6, Long.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(7, String.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(8, String.class, ParameterMode.OUT);
		query.registerStoredProcedureParameter(9, String.class, ParameterMode.OUT);
		query.registerStoredProcedureParameter(10, String.class, ParameterMode.OUT);

		query.setParameter(1, groupPoid);
		query.setParameter(2, loginUserPoid);
		query.setParameter(3, companyPoid);
		query.setParameter(4, bankPoid);
		query.setParameter(5, chqSignType);
		query.setParameter(6, transactionPoid);
		query.setParameter(7, suppressBalanceCheck);

		query.execute();

		Map<String, String> result = new HashMap<>();
		result.put("result", (String) query.getOutputParameterValue(8));
		result.put("nextChequeNumber", (String) query.getOutputParameterValue(9));
		result.put("defaultPrinter", (String) query.getOutputParameterValue(10));
		return result;
	}

	@Override
	public String afterChequePrint(Long groupPoid, String loginUser, Long companyPoid, Long transactionPoid, Long bankPoid,
			String chqSignType, Long userPoid) {
		StoredProcedureQuery query = em.createStoredProcedureQuery("PROC_GL_BANK_AFTER_CHEQ_PRINT");

		query.registerStoredProcedureParameter(1, Long.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(3, Long.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(4, Long.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(5, Long.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(6, String.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(7, Long.class, ParameterMode.IN);
		query.registerStoredProcedureParameter(8, String.class, ParameterMode.OUT);

		query.setParameter(1, groupPoid);
		query.setParameter(2, loginUser);
		query.setParameter(3, companyPoid);
		query.setParameter(4, transactionPoid);
		query.setParameter(5, bankPoid);
		query.setParameter(6, chqSignType);
		query.setParameter(7, userPoid);

		query.execute();
		return  (String) query.getOutputParameterValue(8);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ChequeStockResponse> fetchChequeStock(String bankCode, String signType) {
		StoredProcedureQuery sp = em.createStoredProcedureQuery("PROC_CHEQUE_STOCK_DETAIL_VIEW");
		sp.registerStoredProcedureParameter("P_BANK_CODE", String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter("P_SIGN_TYPE", String.class, ParameterMode.IN);
		sp.registerStoredProcedureParameter("OUTDATA", void.class, ParameterMode.REF_CURSOR);

		String normalizedBankCode = StringUtils.defaultIfBlank(StringUtils.trimToNull(bankCode), "HSBC");
		String normalizedSignType = StringUtils.defaultIfBlank(StringUtils.trimToNull(signType), "NOT_SIGNED");

		sp.setParameter("P_BANK_CODE", normalizedBankCode);
		sp.setParameter("P_SIGN_TYPE", normalizedSignType);

		sp.execute();
		List<Object[]> rows = sp.getResultList();
		List<ChequeStockResponse> out = new ArrayList<>();
		rows.forEach(row -> {
			ChequeStockResponse dto = new ChequeStockResponse();
			dto.setCompany(getString(row, 0));
			dto.setBank(getString(row, 1));
			dto.setStockType(getString(row, 2));
			dto.setTotalChq(getInt(row, 3));
			dto.setFromChq(getString(row, 4));
			dto.setToChq(getString(row, 5));
			dto.setCurrentChq(getString(row, 6));
			dto.setLastChq(getString(row, 7));
			dto.setSelected(getBooleanFromString(row, 8));
			dto.setBankPoid(getString(row, 9));
			dto.setCompanyPoid(getString(row, 10));
			dto.setAvailableBal(getBigDecimal(row, 11));
			dto.setChqCount(getInt(row, 12));
			out.add(dto);
		});
		return out;
	}

	private String getString(Object[] r, int idx) {
		return r.length > idx && r[idx] != null ? r[idx].toString() : null;
	}

	private Long getLong(Object[] r, int idx) {
		if (r.length > idx && r[idx] instanceof Number)
			return ((Number) r[idx]).longValue();
		if (r.length > idx && r[idx] != null) {
			try {
				return Long.parseLong(r[idx].toString());
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	private Integer getInt(Object[] r, int idx) {
		if (r.length > idx && r[idx] instanceof Number)
			return ((Number) r[idx]).intValue();
		if (r.length > idx && r[idx] != null) {
			try {
				return Integer.parseInt(r[idx].toString());
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	private java.util.Date getDate(Object[] r, int idx) {
		if (r.length > idx && r[idx] instanceof java.util.Date)
			return (java.util.Date) r[idx];
		if (r.length > idx && r[idx] instanceof java.sql.Date)
			return new java.util.Date(((java.sql.Date) r[idx]).getTime());
		return null;
	}

	private java.math.BigDecimal getBigDecimal(Object[] r, int idx) {
		if (r.length > idx && r[idx] instanceof java.math.BigDecimal)
			return (java.math.BigDecimal) r[idx];
		if (r.length > idx && r[idx] instanceof Number)
			return java.math.BigDecimal.valueOf(((Number) r[idx]).doubleValue());
		if (r.length > idx && r[idx] != null) {
			try {
				return new java.math.BigDecimal(r[idx].toString());
			} catch (Exception e) {
				return java.math.BigDecimal.ZERO;
			}
		}
		return java.math.BigDecimal.ZERO;
	}

	private Boolean getBooleanFromString(Object[] r, int idx) {
		String s = getString(r, idx);
		if (s == null)
			return Boolean.FALSE;
		return "true".equalsIgnoreCase(s) || "1".equals(s) || Boolean.parseBoolean(s);
	}
}
