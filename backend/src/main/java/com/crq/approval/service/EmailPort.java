package com.crq.approval.service;

import com.crq.approval.model.Crq;
import java.util.List;

public interface EmailPort {
    void sendApprovalEmail(List<Crq> approvedCrqs);
    void sendApprovalEmail(Crq crq);
}
