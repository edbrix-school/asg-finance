package com.asg.finance.repository.reports;

import com.asg.finance.entity.AssetInformationMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetInformationRepository extends JpaRepository<AssetInformationMasterEntity, Long>,
        JpaSpecificationExecutor<AssetInformationMasterEntity> {

    /**
     * Find by IA Code
     */
    Optional<AssetInformationMasterEntity> findByIaCode(String iaCode);

    /**
     * Find by IA Name
     */
    Optional<AssetInformationMasterEntity> findByIaName(String iaName);

    /**
     * Find by IA POID
     */
    Optional<AssetInformationMasterEntity> findByIaPoid(Long iaPoid);

    /**
     * Check if IA Code exists for a different record (for update validation)
     */
    @Query("SELECT COUNT(a) > 0 FROM AssetInformationMasterEntity a WHERE a.iaCode = :iaCode AND a.iaPoid != :iaPoid AND a.deleted = 'N'")
    boolean existsByIaCodeAndIaPoidNot(@Param("iaCode") String iaCode, @Param("iaPoid") Long iaPoid);

    /**
     * Check if IA Name exists for a different record (for update validation)
     */
    @Query("SELECT COUNT(a) > 0 FROM AssetInformationMasterEntity a WHERE a.iaName = :iaName AND a.iaPoid != :iaPoid AND a.deleted = 'N'")
    boolean existsByIaNameAndIaPoidNot(@Param("iaName") String iaName, @Param("iaPoid") Long iaPoid);

    Optional<AssetInformationMasterEntity> findByIaPoidAndDeleted(Long iaPoid, String deleted);

}
