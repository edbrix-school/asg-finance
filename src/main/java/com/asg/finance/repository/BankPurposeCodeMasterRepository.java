package com.asg.finance.repository;

import com.asg.finance.entity.BankPurposeCodeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankPurposeCodeMasterRepository extends JpaRepository<BankPurposeCodeMaster, Long> {

    boolean existsByBankPurposeCodeIgnoreCase(String bankPurposeCode);

    boolean existsByBankPurposePoid(Long bankPurposePoid);
}
