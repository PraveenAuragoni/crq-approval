package com.crq.approval.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "crq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Crq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String crqNumber;

    private String title;

    private String assignee;

    private String description;

    // Status fetched from Remedy API
    private String remedyStatus;

    // Whether this CRQ is approved (status = "Request in Change")
    private boolean approved;

    // Whether approval email was sent
    private boolean emailSent;

    private LocalDateTime emailSentAt;

    // When this CRQ was read from Excel/OneDrive
    private LocalDateTime processedAt;

    // Last updated time on the Excel row (from OneDrive file metadata)
    private LocalDateTime lastUpdatedInExcel;

    // SCHEDULED = daily 5:30 PM run | ADHOC = manual UI trigger
    @Enumerated(EnumType.STRING)
    private BatchType batchType;

    // Batch run identifier (timestamp of the run)
    private LocalDateTime batchRunAt;

    public enum BatchType {
        SCHEDULED, ADHOC
    }
}
