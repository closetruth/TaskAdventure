package com.closetruth.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    @Query("SELECT COALESCE(SUM(t.elapsedSeconds), 0) FROM TaskEntity t WHERE t.status = :status AND t.completedAt >= :start AND t.completedAt < :end")
    long sumElapsedSecondsFinishedBetween(
            @Param("status") TaskStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("SELECT COUNT(t) FROM TaskEntity t WHERE t.status = :status AND t.completedAt >= :start AND t.completedAt < :end")
    long countFinishedBetween(
            @Param("status") TaskStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
