package com.asg.finance.pdcchqbatch.repository;

import com.asg.finance.entity.PdcBatchExcelUploadTemp;
import com.asg.finance.repository.PdcBatchExcelUploadTempRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdcBatchExcelUploadTempRepositoryTest {

    @Mock
    private PdcBatchExcelUploadTempRepository repository;

    @Test
    void saveAllAndFindAll_Success() {
        PdcBatchExcelUploadTemp row = new PdcBatchExcelUploadTemp();
        row.setSn("1");
        row.setChequeNumber("100001");
        row.setChequeDate("2026-04-01");
        row.setChequeAmount("1000");
        row.setDrAmt("1000");
        row.setDrAmt2("0");

        when(repository.saveAll(List.of(row))).thenReturn(List.of(row));
        when(repository.findAll()).thenReturn(List.of(row));

        List<PdcBatchExcelUploadTemp> saved = repository.saveAll(List.of(row));
        List<PdcBatchExcelUploadTemp> found = repository.findAll();

        assertThat(saved).hasSize(1);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getChequeNumber()).isEqualTo("100001");
        verify(repository).saveAll(List.of(row));
        verify(repository).findAll();
    }
}
