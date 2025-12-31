package com.asg.finance.repository;

import com.asg.finance.entity.SupplierServicesMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierServicesMasterRepository extends JpaRepository<SupplierServicesMasterEntity, Long> {
    SupplierServicesMasterEntity findByServicePoid(Long servicePoid);

    boolean existsByServicePoid(Long servicePoid);
}