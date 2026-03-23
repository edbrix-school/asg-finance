package com.asg.finance.repository;

import java.time.LocalDate;
import java.util.Date;
import java.util.Map;

public interface ExpenseReallocationStoredProcedure {

    Map<String, String> createJv(Long groupPoid, Long userPoid, Long companyPoid, Long transactionPoid,
                                 Long expenseGroupGL, LocalDate toDate, String costPoid);

    void generateReport(Long companyPoid, LocalDate toDate, Long expenseGroupGL, Long transactionPoid, String costPoid);
}
