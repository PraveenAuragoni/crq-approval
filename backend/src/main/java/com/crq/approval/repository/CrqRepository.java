package com.crq.approval.repository;

import com.crq.approval.model.Crq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrqRepository extends JpaRepository<Crq, Long> {

    Optional<Crq> findTopByCrqNumberOrderByProcessedAtDesc(String crqNumber);

    List<Crq> findByBatchRunAtOrderByProcessedAtDesc(LocalDateTime batchRunAt);

    List<Crq> findAllByOrderByProcessedAtDesc();

    // CRQs updated in Excel after a given time (for ad-hoc run)
    List<Crq> findByLastUpdatedInExcelAfterOrderByLastUpdatedInExcelDesc(LocalDateTime after);

    // Today's latest batch run
    @Query("SELECT MAX(c.batchRunAt) FROM Crq c WHERE c.batchRunAt >= :startOfDay")
    Optional<LocalDateTime> findLastScheduledRunToday(@Param("startOfDay") LocalDateTime startOfDay);

    List<Crq> findByBatchTypeAndBatchRunAtOrderByProcessedAtDesc(
            Crq.BatchType batchType, LocalDateTime batchRunAt);

    List<Crq> findByProcessedAtBetweenOrderByProcessedAtDesc(
            LocalDateTime startOfDay, LocalDateTime endOfDay);
}
