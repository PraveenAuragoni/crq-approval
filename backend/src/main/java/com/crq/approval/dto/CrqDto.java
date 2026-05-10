package com.crq.approval.dto;

import com.crq.approval.model.Crq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrqDto {
    private Long id;
    private String crqNumber;
    private String title;
    private String assignee;
    private String description;
    private String remedyStatus;
    private boolean approved;
    private boolean emailSent;
    private LocalDateTime emailSentAt;
    private LocalDateTime processedAt;
    private LocalDateTime lastUpdatedInExcel;
    private String batchType;
    private LocalDateTime batchRunAt;

    public static CrqDto from(Crq crq) {
        return CrqDto.builder()
                .id(crq.getId())
                .crqNumber(crq.getCrqNumber())
                .title(crq.getTitle())
                .assignee(crq.getAssignee())
                .description(crq.getDescription())
                .remedyStatus(crq.getRemedyStatus())
                .approved(crq.isApproved())
                .emailSent(crq.isEmailSent())
                .emailSentAt(crq.getEmailSentAt())
                .processedAt(crq.getProcessedAt())
                .lastUpdatedInExcel(crq.getLastUpdatedInExcel())
                .batchType(crq.getBatchType() != null ? crq.getBatchType().name() : null)
                .batchRunAt(crq.getBatchRunAt())
                .build();
    }
}
