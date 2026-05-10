package com.crq.approval.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "processing_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime runAt;

    @Enumerated(EnumType.STRING)
    private Crq.BatchType batchType;

    private int totalCrqsRead;

    private int approvedCount;

    private int emailsSent;

    private String status; // SUCCESS | PARTIAL | FAILED

    @Column(length = 2000)
    private String errorMessage;

    private String triggeredBy; // "SCHEDULER" or user name for ad-hoc
}
