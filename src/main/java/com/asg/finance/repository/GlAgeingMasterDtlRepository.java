package com.asg.finance.repository;

import com.asg.finance.entity.GlAgeingMasterDtlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GlAgeingMasterDtlRepository extends JpaRepository<GlAgeingMasterDtlEntity, GlAgeingMasterDtlEntity.CompositeKey> {
    
    List<GlAgeingMasterDtlEntity> findByAgeingMaster_AgeingPoid(Long ageingPoid);

    Optional<GlAgeingMasterDtlEntity> findByAgeingPoidAndDetRowId(Long ageingPoid, Long detRowId);
    Optional<GlAgeingMasterDtlEntity> findByAgeingMaster_AgeingPoidAndDetRowId(Long ageingPoid, Long detRowId);
    void deleteByAgeingPoidAndDetRowIdIn(Long ageingPoid, List<Long> detRowIds);
    void deleteByAgeingMaster_AgeingPoid(Long ageingPoid);

   }

