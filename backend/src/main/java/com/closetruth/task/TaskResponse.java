package com.closetruth.task;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        TaskStatus status,
        long elapsedSeconds,
        long minutePoints,
        int createBonusPoints,
        long points,
        String createReward,
        String finishReward,
        int finishGoldAwarded,
        int finishDiamondAwarded,
        int accumulatedGold,
        int accumulatedDiamonds,
        Instant firstStartedAt,
        Instant lastPausedAt,
        Instant completedAt
) {
    public static TaskResponse from(TaskEntity task) {
        String createReward = "随机积分 +" + task.getCreateBonusPoints();
        if (task.getCreateRewardMessage() != null && !task.getCreateRewardMessage().isBlank()) {
            createReward += " · " + task.getCreateRewardMessage();
        }
        String finishReward = task.getFinishRewardSummary() != null ? task.getFinishRewardSummary() : "";
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getCurrentElapsedSeconds(),
                task.getMinutePoints(),
                task.getCreateBonusPoints(),
                task.getTotalPoints(),
                createReward,
                finishReward,
                task.getFinishGoldAwarded(),
                task.getFinishDiamondAwarded(),
                task.getAccumulatedGold(),
                task.getAccumulatedDiamonds(),
                task.getFirstStartedAt(),
                task.getLastPausedAt(),
                task.getCompletedAt()
        );
    }
}
