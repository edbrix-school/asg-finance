package com.asg.finance.service.impl;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.service.DocAfterSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocAfterSaveServiceImpl implements DocAfterSaveService {

    private final DataSource dataSource;

    @Override
    public void callDocAfterSave(String docId, Long docKeyPoid) {
        Long groupPoid   = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid    = UserContext.getUserPoid();

        log.info("[DOC_AFTER_SAVE] docId={}, docKeyPoid={}", docId, docKeyPoid);

        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall(
                     "{call PROC_DOC_AFTER_SAVE(?, ?, ?, ?, ?, ?, ?, ?)}")) {

            cs.setLong  (1, groupPoid);
            cs.setLong  (2, companyPoid);
            cs.setLong  (3, userPoid);
            cs.setString(4, docId);
            cs.setLong  (5, docKeyPoid);
            cs.setNull  (6, Types.DATE);
            cs.setNull  (7, Types.VARCHAR);
            cs.registerOutParameter(8, Types.VARCHAR);

            cs.execute();

            String status = cs.getString(8);
            log.info("[DOC_AFTER_SAVE RESULT] status={}", status);

            if (status != null && status.toUpperCase().contains("ERROR")) {
                log.error("[DOC_AFTER_SAVE FAILED] status={}", status);
                throw new ValidationException(status);
            }

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[DOC_AFTER_SAVE ERROR] {}", e.getMessage(), e);
            throw new ValidationException("PROC_DOC_AFTER_SAVE failed: " + e.getMessage());
        }
    }
}
