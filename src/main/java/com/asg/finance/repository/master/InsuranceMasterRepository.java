package com.asg.finance.repository.master;

import com.asg.finance.entity.master.InsuranceMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InsuranceMasterRepository extends JpaRepository<InsuranceMaster, Long> {
    Optional<InsuranceMaster> findById(Long rolePoid);
    boolean existsByPolicyNo(String policyNo);
    boolean existsByPolicyNoAndTransactionPoidNot(String policyNo, Long transactionPoid);
    boolean existsByPolicyNoAndCompanyPoid(String policyNo, Long companyPoid);
    boolean existsByPolicyNoAndCompanyPoidAndTransactionPoidNot(String policyNo, Long companyPoid, Long transactionPoid);

    boolean existsByPolicyNoAndDeletedAndTransactionPoidNot(String policyNo, String deleted, Long transactionPoid);
    
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM InsuranceMaster i WHERE i.transactionPoid = :insuranceId AND i.pjRefPoid IS NOT NULL")
    boolean hasPjReference(@Param("insuranceId") Long insuranceId);
    
    long countByDeleted(String deleted);
    
    List<InsuranceMaster> findByExpiryDateBetweenAndDeleted(LocalDate startDate, LocalDate endDate, String deleted);
}