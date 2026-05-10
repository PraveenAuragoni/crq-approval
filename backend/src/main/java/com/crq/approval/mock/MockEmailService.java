package com.crq.approval.mock;

import com.crq.approval.model.Crq;
import com.crq.approval.service.EmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Profile("mock")
public class MockEmailService implements EmailPort {

    @Override
    public void sendApprovalEmail(List<Crq> approvedCrqs) {
        log.info("[MOCK] Email would be sent for {} approved CRQ(s):", approvedCrqs.size());
        approvedCrqs.forEach(c ->
            log.info("[MOCK]   → {} | {} | Status: {}", c.getCrqNumber(), c.getTitle(), c.getRemedyStatus())
        );
    }

    @Override
    public void sendApprovalEmail(Crq crq) {
        log.info("[MOCK] Email would be sent for CRQ: {} - {}", crq.getCrqNumber(), crq.getTitle());
    }
}
