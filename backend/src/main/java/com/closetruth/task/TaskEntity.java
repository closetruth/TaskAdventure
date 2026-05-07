package com.closetruth.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.CREATED;

    @Column(nullable = false)
    private long elapsedSeconds;

    private Instant runningSince;

    @Column(nullable = false)
    private int createBonusPoints;

    @Column(length = 128)
    private String createRewardMessage;

    @Column(nullable = false)
    private int finishGoldAwarded;

    @Column(nullable = false)
    private int finishDiamondAwarded;

    @Column(length = 512)
    private String finishRewardSummary;

    private Instant firstStartedAt;
    private Instant lastPausedAt;
    private Instant completedAt;

    // ... new persisted accumulators (client-side ticks will be stored here)
    @Column
    private Integer accumulatedGold;

    @Column
    private Integer accumulatedDiamonds;

    protected TaskEntity() {
    }

    public TaskEntity(String title, int createBonusPoints, String createRewardMessage) {
        this.title = title;
        this.createBonusPoints = createBonusPoints;
        this.createRewardMessage = createRewardMessage;
        this.status = TaskStatus.CREATED;
        this.elapsedSeconds = 0;
        this.finishGoldAwarded = 0;
        this.finishDiamondAwarded = 0;
        this.accumulatedGold = 0;
        this.accumulatedDiamonds = 0;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public Instant getRunningSince() {
        return runningSince;
    }

    public int getCreateBonusPoints() {
        return createBonusPoints;
    }

    public String getCreateRewardMessage() {
        return createRewardMessage;
    }

    public int getFinishGoldAwarded() {
        return finishGoldAwarded;
    }

    public int getFinishDiamondAwarded() {
        return finishDiamondAwarded;
    }

    public String getFinishRewardSummary() {
        return finishRewardSummary;
    }

    public Instant getFirstStartedAt() {
        return firstStartedAt;
    }

    public Instant getLastPausedAt() {
        return lastPausedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public int getAccumulatedGold() {
        return accumulatedGold == null ? 0 : accumulatedGold;
    }

    public int getAccumulatedDiamonds() {
        return accumulatedDiamonds == null ? 0 : accumulatedDiamonds;
    }

    public void addAccumulated(int gold, int diamonds) {
        if (gold > 0) {
            if (this.accumulatedGold == null) this.accumulatedGold = 0;
            this.accumulatedGold += gold;
        }
        if (diamonds > 0) {
            if (this.accumulatedDiamonds == null) this.accumulatedDiamonds = 0;
            this.accumulatedDiamonds += diamonds;
        }
    }

    public void clearAccumulated() {
        this.accumulatedGold = 0;
        this.accumulatedDiamonds = 0;
    }

    public void start() {
        if (status != TaskStatus.CREATED && status != TaskStatus.PAUSED) {
            return;
        }
        if (firstStartedAt == null) {
            firstStartedAt = Instant.now();
        }
        status = TaskStatus.RUNNING;
        runningSince = Instant.now();
    }

    public void pause() {
        if (status != TaskStatus.RUNNING) {
            return;
        }
        syncElapsedSeconds();
        status = TaskStatus.PAUSED;
        lastPausedAt = Instant.now();
        runningSince = null;
    }

    public void applyFinishRewards(String summary, int gold, int diamonds) {
        if (status == TaskStatus.FINISHED) {
            return;
        }
        if (status == TaskStatus.RUNNING) {
            syncElapsedSeconds();
        }
        status = TaskStatus.FINISHED;
        runningSince = null;
        completedAt = Instant.now();
        finishRewardSummary = summary;
        finishGoldAwarded = gold;
        finishDiamondAwarded = diamonds;
        // accumulated are expected to have been included by service; ensure cleared
        clearAccumulated();
    }

    public long getCurrentElapsedSeconds() {
        if (status != TaskStatus.RUNNING || runningSince == null) {
            return elapsedSeconds;
        }
        long delta = Instant.now().getEpochSecond() - runningSince.getEpochSecond();
        return elapsedSeconds + Math.max(delta, 0);
    }

    // Per new requirement: minutes no longer contribute to points used for finish rolls
    public long getMinutePoints() {
        return 0L;
    }

    public long getTotalPoints() {
        // Now only creation bonus counts as "points" for finish roll
        return createBonusPoints;
    }

    private void syncElapsedSeconds() {
        elapsedSeconds = getCurrentElapsedSeconds();
    }
}
