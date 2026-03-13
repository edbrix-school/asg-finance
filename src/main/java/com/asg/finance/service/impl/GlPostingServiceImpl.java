package com.asg.finance.service.impl;

import com.asg.common.lib.exception.ValidationException;
import com.asg.common.lib.security.util.UserContext;
import com.asg.finance.service.GlPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

@Service
@Slf4j
@RequiredArgsConstructor
public class GlPostingServiceImpl implements GlPostingService {

    private final DataSource dataSource;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public String performGlPosting(String docId, Long transactionPoid, String docRef) {
        Long groupPoid = UserContext.getGroupPoid();
        Long companyPoid = UserContext.getCompanyPoid();
        Long userPoid = UserContext.getUserPoid();

        if (transactionPoid == null || transactionPoid <= 0) {
            throw new ValidationException("Invalid DocumentKeyPoid for GLPosting");
        }

        log.info("Performing GL posting for docId: {}, transactionPoid: {}, docRef: {}", docId, transactionPoid, docRef);

        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (CallableStatement cs = connection.prepareCall("{call PROC_GL_LEDGER_POSTING_MAIN(?, ?, ?, ?, ?, ?, ?)}")) {

            cs.setLong(1, groupPoid);
            cs.setLong(2, companyPoid);
            cs.setLong(3, userPoid);
            cs.setString(4, docId);
            cs.setLong(5, transactionPoid);
            cs.setString(6, docRef);
            cs.registerOutParameter(7, Types.VARCHAR);

            cs.execute();
            String result = cs.getString(7);
            log.info("GL posting completed successfully: {}", result);
            return result;

        } catch (SQLException e) {
            log.error("SQL error during GL posting: {}", e.getMessage(), e);
            throw new ValidationException("GL posting failed: " + e.getMessage());
        }
    }
}
