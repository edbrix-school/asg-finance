package com.asg.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "Search filters for attachments")
public class AttachmentSearchCriteria {
    private Long docKeyPoid;                 // required for narrowing to one transaction
    private String docId;                    // optional
    private String q;                        // global text: file name, remarks, checklist, createdBy
    private String checklistName;            // optional
    private String uploadedBy;               // optional
    private Boolean includeArchived = false; // include archived/old records

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdFrom;           // optional

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdTo;             // optional
}
