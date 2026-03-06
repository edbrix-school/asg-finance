package com.asg.finance.service;

import com.asg.common.lib.dto.DeleteReasonDto;
import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.finance.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface GlRecurringJvService {

    Map<String, Object> listRecurringJvs(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);

    RecurringJvResponse getRecurringJvById(Long transactionPoid);

    RecurringJvCreateResponse createRecurringJv(RecurringJvRequest request, String docId);

    RecurringJvCreateResponse updateRecurringJv(Long transactionPoid, RecurringJvRequest request, String docId);

    void deleteRecurringJv(Long transactionPoid, DeleteReasonDto deleteReasonDto);

    CreateScheduleResponse createSchedule(Long transactionPoid, CreateScheduleRequest request);

    void deleteSchedule(Long transactionPoid);

    byte[] print(Long transactionPoid) throws Exception;

}
