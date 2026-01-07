package com.asg.finance.service;

import com.asg.finance.dto.CreateScheduleRequest;
import com.asg.finance.dto.CreateScheduleResponse;
import com.asg.finance.dto.RecurringJvCreateResponse;
import com.asg.finance.dto.RecurringJvRequest;
import com.asg.finance.dto.RecurringJvResponse;
import com.asg.common.lib.dto.FilterRequestDto;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;

public interface GlRecurringJvService {
    
    Map<String, Object> listRecurringJvs(String documentId, FilterRequestDto filters, LocalDate startDate, LocalDate endDate, Pageable pageable);

    RecurringJvResponse getRecurringJvById(Long transactionPoid);
    
    RecurringJvCreateResponse createRecurringJv(RecurringJvRequest request,String docId);
    
    RecurringJvCreateResponse updateRecurringJv(Long transactionPoid, RecurringJvRequest request,String docId);
    
    void deleteRecurringJv(Long transactionPoid);
    
    CreateScheduleResponse createSchedule(Long transactionPoid, CreateScheduleRequest request);
    
    void deleteSchedule(Long transactionPoid);

    byte[] print(Long transactionPoid) throws Exception;

}
