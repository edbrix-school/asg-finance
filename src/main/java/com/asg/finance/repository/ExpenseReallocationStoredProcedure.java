package com.asg.finance.repository;

import java.util.Date;
import java.util.Map;

public interface ExpenseReallocationStoredProcedure {

	Map<String, String> createJv(Long groupPoid, Long userPoid, Long companyPoid, Long transactionPoid,
			Long expenseGroupGL, Date toDate, String costPoid);

	void generateReport(Long companyPoid, Date toDate, Long expenseGroupGL, Long transactionPoid, String costPoid);
}
