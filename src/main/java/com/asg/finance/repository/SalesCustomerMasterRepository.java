package com.asg.finance.repository;

import com.asg.finance.entity.SalesCustomerMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesCustomerMasterRepository extends JpaRepository<SalesCustomerMaster, Long> {
    SalesCustomerMaster findByCustomerPoid(Long customerPoid);

    Boolean existsByCustomerPoid(Long customerPoid);
}

