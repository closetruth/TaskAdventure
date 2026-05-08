package com.closetruth.task;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        TaskStatus status,
        long elapsedSeconds,
        int finishGoldAwarded,
        int finishDiamondAwarded,
        int accumulatedGold,
        int accumulatedDiamonds,
        int pendingGold,
        int pendingDiamonds,
        Instant createdAt,
        Instant completedAt
) {
    public static TaskResponse from(TaskEntity task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getCurrentElapsedSeconds(),
                task.getFinishGoldAwarded(),
                task.getFinishDiamondAwarded(),
                task.getAccumulatedGold(),
                task.getAccumulatedDiamonds(),
                task.getPendingGold(),
                task.getPendingDiamonds(),
                task.getCreatedAt(),
                task.getCompletedAt()
        );
    }
}
