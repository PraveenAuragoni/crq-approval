package com.crq.approval.repository;

import com.crq.approval.model.ProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessingLogRepository extends JpaRepository<ProcessingLog, Long> {

    List<ProcessingLog> findAllByOrderByRunAtDesc();

    List<ProcessingLog> findTop10ByOrderByRunAtDesc();
}
