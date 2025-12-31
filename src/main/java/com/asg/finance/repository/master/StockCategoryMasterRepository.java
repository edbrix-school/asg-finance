package com.asg.finance.repository.master;


import com.asg.finance.entity.master.StockCategoryMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StockCategoryMasterRepository extends JpaRepository<StockCategoryMasterEntity, Long> {
    Optional<StockCategoryMasterEntity> findByCategoryPoid(Long categoryPoid);
    List<StockCategoryMasterEntity> findByCategoryPoidIn(Set<Long> categoryPoids);
}
