package com.asg.finance.repository;

import com.asg.finance.entity.StockMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StockMasterRepository extends JpaRepository<StockMasterEntity, Long> {
    Optional<StockMasterEntity> findByStockPoid(Long stockPoid);

    List<StockMasterEntity> findByStockPoidIn(Set<Long> stockPoids);

//    boolean existsByStockPoid(Long stockPoid);
}
