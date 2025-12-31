package com.asg.finance.repository;

import com.asg.finance.entity.GlAgeingMasterEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GlAgeingMasterRepository extends JpaRepository<GlAgeingMasterEntity, Long> {
    
    GlAgeingMasterEntity findByAgeingPoid(Long ageingPoid);
    
    boolean existsByDescription(String description);
    
    boolean existsByDescriptionAndAgeingPoidNot(String description, Long ageingPoid);


    @Query("SELECT g FROM GlAgeingMasterEntity g WHERE " +
            "LOWER(g.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(g.description2) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(g.ageingBreakupType) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<GlAgeingMasterEntity> search(@Param("search") String search, Pageable pageable);
}
