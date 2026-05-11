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

    // Type/category from Excel (Standard, Emergency, …) — used as the display title
    private String title;

    // Application / team name from Excel col C
    private String application;

    // Country from Excel col D
    private String country;

    // CRQ type from Excel col F (mirrors title for backward compatibility)
    private String crqType;

    // Created flag from Excel col B (Yes / No)
    private String created;

    private String description;

    // Status fetched from Remedy API
    private String remedyStatus;

    // Whether this CRQ is approved (status == configured approved-status)
    private boolean approved;

    // Whether approval email was sent
    private boolean emailSent;

    private LocalDateTime emailSentAt;

    // When this CRQ was read/processed
    private LocalDateTime processedAt;

    // Date of the green date-separator row this CRQ belongs to
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
