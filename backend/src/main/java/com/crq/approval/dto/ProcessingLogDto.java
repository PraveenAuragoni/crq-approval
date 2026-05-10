package com.crq.approval.dto;

import com.crq.approval.model.ProcessingLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingLogDto {
    private Long id;
    private LocalDateTime runAt;
    private String batchType;
    private int totalCrqsRead;
    private int approvedCount;
    private int emailsSent;
    private String status;
    private String errorMessage;
    private String triggeredBy;

    public static ProcessingLogDto from(ProcessingLog log) {
        return ProcessingLogDto.builder()
                .id(log.getId())
                .runAt(log.getRunAt())
                .batchType(log.getBatchType() != null ? log.getBatchType().name() : null)
                .totalCrqsRead(log.getTotalCrqsRead())
                .approvedCount(log.getApprovedCount())
                .emailsSent(log.getEmailsSent())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .triggeredBy(log.getTriggeredBy())
                .build();
    }
}
