package com.asg.finance.repository;

import org.springframework.stereotype.Repository;

@Repository
public interface GlRecurringJvProcRepository {
    void createSchedule(Long groupPoid, Long userPoid, Long companyPoid, Long transactionPoid);
    
    void deleteSchedule(Long groupPoid, Long userPoid, Long companyPoid, Long transactionPoid);
}
