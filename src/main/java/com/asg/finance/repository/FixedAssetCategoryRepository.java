package com.asg.finance.repository;

import com.asg.finance.entity.FixedAssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FixedAssetCategoryRepository extends JpaRepository<FixedAssetCategory, Long> {
    Optional<FixedAssetCategory> findByFaCategoryPoid(Long faCategoryPoid);
    boolean existsByFaCategoryCodeIgnoreCase(String faCategoryCode);
    boolean existsByFaCategoryDescriptionIgnoreCase(String faCategoryDescription);
    boolean existsByFaCategoryCodeIgnoreCaseAndFaCategoryPoidNot(String faCategoryCode, Long faCategoryPoid);
    boolean existsByFaCategoryDescriptionIgnoreCaseAndFaCategoryPoidNot(String faCategoryDescription, Long faCategoryPoid);
}
