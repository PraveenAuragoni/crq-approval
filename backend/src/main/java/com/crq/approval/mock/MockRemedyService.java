package com.crq.approval.mock;

import com.crq.approval.service.RemedyPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@Profile("mock")
public class MockRemedyService implements RemedyPort {

    private static final String APPROVED = "Request in Change";

    // Predefined mock statuses: 6 approved, 4 not
    private static final Map<String, String> STATUS_MAP = Map.of(
        "CRQ000123001", APPROVED,
        "CRQ000123002", APPROVED,
        "CRQ000123003", APPROVED,
        "CRQ000123004", "Planning",
        "CRQ000123005", APPROVED,
        "CRQ000123006", APPROVED,
        "CRQ000123007", "Draft",
        "CRQ000123008", APPROVED,
        "CRQ000123009", "Pending Approval",
        "CRQ000123010", "Draft"
    );

    @Override
    public String getCrqStatus(String crqNumber) {
        String status = STATUS_MAP.getOrDefault(crqNumber, "Planning");
        log.info("[MOCK] Remedy status for {}: {}", crqNumber, status);
        return status;
    }

    @Override
    public boolean isApproved(String status) {
        return APPROVED.equalsIgnoreCase(status);
    }
}
