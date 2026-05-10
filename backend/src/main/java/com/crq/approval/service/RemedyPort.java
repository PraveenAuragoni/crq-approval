package com.crq.approval.service;

public interface RemedyPort {
    String getCrqStatus(String crqNumber);
    boolean isApproved(String status);
}
