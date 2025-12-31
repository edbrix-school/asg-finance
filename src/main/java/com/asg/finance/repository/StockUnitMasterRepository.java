package com.asg.finance.repository;

import com.asg.finance.entity.StockUnitMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockUnitMasterRepository extends JpaRepository<StockUnitMaster, Long> {
    boolean existsByStockUnitPoid(Long stockUnitPoid);
}

