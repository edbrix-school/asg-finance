package com.asg.finance.service;

import com.asg.common.lib.dto.FilterRequestDto;
import com.asg.common.lib.dto.RawSearchResult;
import com.asg.common.lib.exception.ResourceAlreadyExistsException;
import com.asg.common.lib.service.DocumentSearchService;
import com.asg.finance.dto.BankPayeeRequest;
import com.asg.finance.dto.BankPayeeResponse;
import com.asg.finance.entity.BankPayee;
import com.asg.finance.repository.BankPayeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankPayeeServiceTest {

    @Mock(lenient = true)
    private BankPayeeRepository repository;

    @Mock(lenient = true)
    private DocumentSearchService documentService;

    @Mock(lenient = true)
    private Pageable pageable;

    @InjectMocks
    private BankPayeeServiceImpl bankPayeeService;

    private BankPayeeRequest request;
    private BankPayee entity;

    @BeforeEach
    void setUp() {
        request = new BankPayeeRequest();
        request.setPayingName("Al-wasim Shekh");
        request.setPayingName2("Al-Asim");
        request.setRemarks("Cheque print name");
        request.setActive("Y");

        entity = new BankPayee();
        entity.setPayingPoid(1L);
        entity.setPayingName("Al-wasim Shekh");
        entity.setPayingName2("Al-Asim");
        entity.setRemarks("Cheque print name");
        entity.setActive("Y");
    }

    @Test
    void createPayee_ShouldReturnResponse_WhenPayeeIsValid() {
        when(repository.findByPayingName(request.getPayingName())).thenReturn(Optional.empty());
        when(repository.save(any(BankPayee.class))).thenReturn(entity);

        BankPayeeResponse response = bankPayeeService.createPayee(request);

        assertNotNull(response);
        assertEquals("Al-wasim Shekh", response.getPayingName());
        assertEquals("Y", response.getActive());
        assertEquals(1L, response.getPoid());
    }

    @Test
    void createPayee_ShouldThrowException_WhenPayeeExists() {
        when(repository.findByPayingName(request.getPayingName())).thenReturn(Optional.of(entity));

        assertThrows(ResourceAlreadyExistsException.class, () -> bankPayeeService.createPayee(request));
    }

    @Test
    void createPayee_ShouldSetDefaultActive_WhenActiveIsNull() {
        request.setActive(null);
        when(repository.findByPayingName(request.getPayingName())).thenReturn(Optional.empty());
        when(repository.save(any(BankPayee.class))).thenReturn(entity);

        BankPayeeResponse response = bankPayeeService.createPayee(request);

        assertNotNull(response);
        verify(repository).save(argThat(payee -> "N".equals(payee.getActive())));
    }

    @Test
    void createPayee_ShouldThrowException_WhenActiveIsInvalid() {
        request.setActive("X");

        assertThrows(ResponseStatusException.class, () -> bankPayeeService.createPayee(request));
    }

    @Test
    void createPayee_ShouldAcceptActiveN() {
        request.setActive("N");
        when(repository.findByPayingName(request.getPayingName())).thenReturn(Optional.empty());
        when(repository.save(any(BankPayee.class))).thenReturn(entity);

        BankPayeeResponse response = bankPayeeService.createPayee(request);

        assertNotNull(response);
        verify(repository).save(argThat(payee -> "N".equals(payee.getActive())));
    }

    @Test
    void createPayee_ShouldSetDeletedToN() {
        when(repository.findByPayingName(request.getPayingName())).thenReturn(Optional.empty());
        when(repository.save(any(BankPayee.class))).thenReturn(entity);

        bankPayeeService.createPayee(request);

        verify(repository).save(argThat(payee -> "N".equals(payee.getDeleted())));
    }

    @Test
    void createPayee_ShouldHandleEmptyPayingName2() {
        request.setPayingName2("");
        when(repository.findByPayingName(request.getPayingName())).thenReturn(Optional.empty());
        when(repository.save(any(BankPayee.class))).thenReturn(entity);

        BankPayeeResponse response = bankPayeeService.createPayee(request);

        assertNotNull(response);
        verify(repository).save(argThat(payee -> "".equals(payee.getPayingName2())));
    }

    @Test
    void createPayee_ShouldHandleNullRemarks() {
        request.setRemarks(null);
        when(repository.findByPayingName(request.getPayingName())).thenReturn(Optional.empty());
        when(repository.save(any(BankPayee.class))).thenReturn(entity);

        BankPayeeResponse response = bankPayeeService.createPayee(request);

        assertNotNull(response);
        verify(repository).save(argThat(payee -> payee.getRemarks() == null));
    }

    @Test
    void softDeleteBypPayingPoid_ShouldMarkAsDeleted_WhenPayeeExists() {
        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));

        bankPayeeService.softDeleteBypPayingPoid(1L);

        verify(repository).save(argThat(payee ->
                "N".equals(payee.getActive()) && "Y".equals(payee.getDeleted())));
    }

    @Test
    void softDeleteBypPayingPoid_ShouldThrowException_WhenPayeeNotFound() {
        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> bankPayeeService.softDeleteBypPayingPoid(1L));
    }

    @Test
    void softDeleteBypPayingPoid_ShouldSetModificationFields() {
        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));

        bankPayeeService.softDeleteBypPayingPoid(1L);

        verify(repository).save(argThat(payee ->
                payee.getLastModifiedDate() != null && payee.getLastModifiedBy() != null));
    }

    @Test
    void updatePayee_ShouldReturnResponse_WhenPayeeExists() {
        BankPayeeRequest updateRequest = new BankPayeeRequest();
        updateRequest.setPayingName("Updated Name");
        updateRequest.setActive("Y");

        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));
        when(repository.existsByPayingNameIgnoreCaseAndDeleted("Updated Name", "N")).thenReturn(false);

        BankPayeeResponse response = bankPayeeService.updatePayee(1L, updateRequest);

        assertNotNull(response);
        verify(repository).save(any(BankPayee.class));
    }

    @Test
    void updatePayee_ShouldThrowException_WhenPayeeNotFound() {
        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                bankPayeeService.updatePayee(1L, new BankPayeeRequest()));
    }

    @Test
    void updatePayee_ShouldThrowException_WhenPayingNameExists() {
        BankPayeeRequest updateRequest = new BankPayeeRequest();
        updateRequest.setPayingName("Existing Name");

        entity.setPayingName("Different Name");
        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));
        when(repository.existsByPayingNameIgnoreCaseAndDeleted("Existing Name", "N")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                bankPayeeService.updatePayee(1L, updateRequest));
    }

    @Test
    void updatePayee_ShouldNotCheckDuplicateWhenSameName() {
        BankPayeeRequest updateRequest = new BankPayeeRequest();
        updateRequest.setPayingName("Al-wasim Shekh");

        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));

        BankPayeeResponse response = bankPayeeService.updatePayee(1L, updateRequest);

        assertNotNull(response);
        verify(repository, never()).existsByPayingNameIgnoreCaseAndDeleted(anyString(), anyString());
    }

    @Test
    void updatePayee_ShouldThrowException_WhenActiveIsInvalid() {
        BankPayeeRequest updateRequest = new BankPayeeRequest();
        updateRequest.setActive("X");

        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));

        assertThrows(ResponseStatusException.class, () ->
                bankPayeeService.updatePayee(1L, updateRequest));
    }

    @Test
    void updatePayee_ShouldUpdateOnlyProvidedFields() {
        BankPayeeRequest updateRequest = new BankPayeeRequest();
        updateRequest.setRemarks("New Remarks");

        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));

        BankPayeeResponse response = bankPayeeService.updatePayee(1L, updateRequest);

        assertNotNull(response);
        verify(repository).save(argThat(payee ->
                "New Remarks".equals(payee.getRemarks()) &&
                        "Al-wasim Shekh".equals(payee.getPayingName())));
    }

    @Test
    void updatePayee_ShouldSetModificationFields() {
        BankPayeeRequest updateRequest = new BankPayeeRequest();
        updateRequest.setActive("N");

        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));

        bankPayeeService.updatePayee(1L, updateRequest);

        verify(repository).save(argThat(payee ->
                payee.getLastModifiedDate() != null && payee.getLastModifiedBy() != null));
    }

    @Test
    void updatePayee_ShouldHandleNullFields() {
        BankPayeeRequest updateRequest = new BankPayeeRequest();
        // All fields are null

        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));

        BankPayeeResponse response = bankPayeeService.updatePayee(1L, updateRequest);

        assertNotNull(response);
        verify(repository).save(any(BankPayee.class));
    }

    @Test
    void updatePayee_ShouldHandleCaseInsensitiveNameCheck() {
        BankPayeeRequest updateRequest = new BankPayeeRequest();
        updateRequest.setPayingName("AL-WASIM SHEKH");

        entity.setPayingName("al-wasim shekh");
        when(repository.findByPayingPoidAndDeleted(1L, "N")).thenReturn(Optional.of(entity));

        BankPayeeResponse response = bankPayeeService.updatePayee(1L, updateRequest);

        assertNotNull(response);
        verify(repository, never()).existsByPayingNameIgnoreCaseAndDeleted(anyString(), anyString());
    }

    @Test
    void listPayees_ShouldReturnPagedResults() {
        FilterRequestDto filters = new FilterRequestDto("AND", "N", List.of());
        RawSearchResult rawResult = new RawSearchResult(List.of(), Map.of(), 0L);

        when(documentService.resolveOperator(filters)).thenReturn("AND");
        when(documentService.resolveIsDeleted(filters)).thenReturn("N");
        when(documentService.resolveFilters(filters)).thenReturn(List.of());

        when(documentService.search(anyString(), any(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = bankPayeeService.listPayees("000-001", filters, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("000-001"), any(), eq("AND"), eq(pageable),
                eq("N"), eq("PAYING_NAME"), eq("PAYING_POID"));
    }

    @Test
    void listPayees_ShouldHandleNullFilters() {
        RawSearchResult rawResult = new RawSearchResult(List.of(), Map.of(), 0L);

        when(documentService.resolveOperator(null)).thenReturn("OR");
        when(documentService.resolveIsDeleted(null)).thenReturn("N");
        when(documentService.resolveFilters(null)).thenReturn(List.of());

        when(documentService.search(anyString(), any(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = bankPayeeService.listPayees("000-001", null, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("000-001"), any(), eq("OR"), eq(pageable),
                eq("N"), eq("PAYING_NAME"), eq("PAYING_POID"));
    }

    @Test
    void listPayees_ShouldHandleEmptyResults() {
        FilterRequestDto filters = new FilterRequestDto("OR", "Y", List.of());
        RawSearchResult rawResult = new RawSearchResult(List.of(), Map.of("name", "Paying Name"), 5L);

        when(documentService.resolveOperator(filters)).thenReturn("OR");
        when(documentService.resolveIsDeleted(filters)).thenReturn("Y");
        when(documentService.resolveFilters(filters)).thenReturn(List.of());

        when(documentService.search(anyString(), any(), anyString(), any(Pageable.class),
                anyString(), anyString(), anyString())).thenReturn(rawResult);

        Map<String, Object> result = bankPayeeService.listPayees("000-001", filters, pageable);

        assertNotNull(result);
        verify(documentService).search(eq("000-001"), any(), eq("OR"), eq(pageable),
                eq("Y"), eq("PAYING_NAME"), eq("PAYING_POID"));
    }
}
