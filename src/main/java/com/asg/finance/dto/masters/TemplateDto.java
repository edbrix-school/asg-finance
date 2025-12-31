package com.asg.finance.dto.masters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemplateDto {

    private String templateId;
    private Long termsPoid;
    private String docId;
    private String templateName;

}
