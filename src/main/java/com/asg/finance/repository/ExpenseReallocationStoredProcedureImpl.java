package com.asg.finance.repository;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class ExpenseReallocationStoredProcedureImpl implements ExpenseReallocationStoredProcedure {

	@PersistenceContext
	private EntityManager em;

	// ===================== CREATE JV =====================

	@Override
	public Map<String, String> createJv(Long groupPoid, Long userPoid, Long companyPoid, Long transactionPoid,
			Long expenseGroupGL, Date toDate, String costPoid) {

		return em.unwrap(Session.class).doReturningWork(connection -> {

			final String sql = "{ call PROC_GL_EXPENSE_REALLOCTN_XL_JV(?, ?, ?, ?, ?, ?, ?, ?, ?) }";

			try (CallableStatement cs = connection.prepareCall(sql)) {

				cs.setLong(1, groupPoid);
				cs.setLong(2, userPoid);
				cs.setLong(3, companyPoid);
				cs.setLong(4, transactionPoid);

				setLongOrNull(cs, 5, expenseGroupGL);
				setDateOrNull(cs, 6, toDate);
				cs.setString(7, costPoid);

				cs.registerOutParameter(8, Types.VARCHAR);
				cs.registerOutParameter(9, Types.VARCHAR);

				cs.execute();

				String result = cs.getString(8);
				String docRef = cs.getString(9);

				log.debug("JV SP Result={}, DocRef={}", result, docRef);

				if (result != null && result.toLowerCase().startsWith("error")) {
					throw new RuntimeException("JV Creation Failed: " + result);
				}

				Map<String, String> response = new HashMap<>();
				response.put("result", result);
				response.put("docRef", docRef);
				return response;
			}
		});
	}

	// ===================== GENERATE REPORT =====================

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void generateReport(Long companyPoid, Date toDate, Long expenseGroupGL, Long transactionPoid,
			String costPoid) {

		log.info(
				"Executing Expense Allocation Report SP | companyPoid={} toDate={} expenseGroupGL={} transactionPoid={} costPoid={}",
				companyPoid, toDate, expenseGroupGL, transactionPoid, costPoid);

		em.flush();
		em.clear();

		em.unwrap(Session.class).doWork(connection -> {

			final String sql = "{ call PRODUCTION.PROC_DYN_EXPENSE_ALLOCATN_XL_RPT(?, ?, ?, ?, ?) }";

			try (CallableStatement cs = connection.prepareCall(sql)) {

				int idx = 1;
				setLongOrNull(cs, idx++, companyPoid);
				setDateOrNull(cs, idx++, toDate);
				setLongOrNull(cs, idx++, expenseGroupGL);
				setLongOrNull(cs, idx++, transactionPoid);
				setStringOrNull(cs, idx, costPoid);

				cs.execute();

				log.debug("Expense Allocation Report SP executed successfully");

			} catch (SQLException e) {
				log.error("Error executing PROC_DYN_EXPENSE_ALLOCATN_XL_RPT", e);
				throw new RuntimeException("Failed to generate expense allocation report", e);
			}
		});
	}

	// ===================== HELPER METHODS =====================

	private void setLongOrNull(CallableStatement cs, int index, Long value) throws SQLException {
		if (value != null) {
			cs.setLong(index, value);
		} else {
			cs.setNull(index, Types.NUMERIC);
		}
	}

	private void setDateOrNull(CallableStatement cs, int index, Date value) throws SQLException {
		if (value != null) {
			cs.setDate(index, new java.sql.Date(value.getTime()));
		} else {
			cs.setNull(index, Types.DATE);
		}
	}

	private void setStringOrNull(CallableStatement cs, int index, String value) throws SQLException {
		if (value != null && !value.trim().isEmpty()) {
			cs.setString(index, value);
		} else {
			cs.setNull(index, Types.VARCHAR);
		}
	}
}
