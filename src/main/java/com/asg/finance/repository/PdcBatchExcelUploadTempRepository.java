package com.asg.finance.repository;

import com.asg.finance.entity.PdcBatchExcelUploadTemp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PdcBatchExcelUploadTempRepository
        extends JpaRepository<PdcBatchExcelUploadTemp, String> {

}
