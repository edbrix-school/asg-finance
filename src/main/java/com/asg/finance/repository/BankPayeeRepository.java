package com.asg.finance.repository;

import com.asg.finance.entity.BankPayee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BankPayeeRepository extends JpaRepository<BankPayee, Long>, JpaSpecificationExecutor<BankPayee> {

    Optional<BankPayee> findByPayingName(String payingName);

    Optional<BankPayee> findByPayingPoidAndActive(Long id, String active);
    Optional<BankPayee> findByPayingPoidAndDeleted(Long payingPoid, String deleted);
    boolean existsByPayingNameIgnoreCaseAndDeleted(String payingName, String deleted);

    Optional<BankPayee> findByPayingPoid(Long payingPoid);
}
