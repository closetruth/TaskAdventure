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
    private Instant createdAt;

    private Instant completedAt;

    @Column(nullable = false)
    private int finishGoldAwarded;

    @Column(nullable = false)
    private int finishDiamondAwarded;

    // ... new persisted accumulators (client-side ticks will be stored here)
    @Column
    private Integer accumulatedGold;

    @Column
    private Integer accumulatedDiamonds;

    // Pending rewards that need to be claimed manually (10-minute cycle rewards)
    @Column
    private int pendingGold;

    @Column
    private int pendingDiamonds;

    // Planned duration in minutes (for auto-pause and bonus calculation)
    @Column
    private Integer plannedMinutes;

    protected TaskEntity() {
    }

    public TaskEntity(String title) {
        this.title = title;
        this.status = TaskStatus.CREATED;
        this.elapsedSeconds = 0;
        this.createdAt = Instant.now();
        this.finishGoldAwarded = 0;
        this.finishDiamondAwarded = 0;
        this.accumulatedGold = 0;
        this.accumulatedDiamonds = 0;
        this.pendingGold = 0;
        this.pendingDiamonds = 0;
        this.plannedMinutes = null;
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

    public Instant getCreatedAt() {
        return createdAt;
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

    public int getPendingGold() {
        return pendingGold;
    }

    public int getPendingDiamonds() {
        return pendingDiamonds;
    }

    public Integer getPlannedMinutes() {
        return plannedMinutes;
    }

    public void setPlannedMinutes(Integer plannedMinutes) {
        this.plannedMinutes = plannedMinutes;
    }

    public boolean shouldAutoPause() {
        if (plannedMinutes == null || plannedMinutes <= 0) {
            return false;
        }
        long currentElapsed = getCurrentElapsedSeconds();
        long plannedSeconds = plannedMinutes * 60L;
        return currentElapsed >= plannedSeconds;
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

    public void addPendingRewards(int gold, int diamonds) {
        if (gold > 0) {
            this.pendingGold += gold;
        }
        if (diamonds > 0) {
            this.pendingDiamonds += diamonds;
        }
    }

    public void claimPendingRewards() {
        this.pendingGold = 0;
        this.pendingDiamonds = 0;
    }

    public void start() {
        if (status != TaskStatus.CREATED && status != TaskStatus.PAUSED) {
            return;
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
        runningSince = null;
    }

    public void applyFinishRewards(int gold, int diamonds) {
        if (status == TaskStatus.FINISHED) {
            return;
        }
        if (status == TaskStatus.RUNNING) {
            syncElapsedSeconds();
        }
        status = TaskStatus.FINISHED;
        runningSince = null;
        completedAt = Instant.now();
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

    private void syncElapsedSeconds() {
        elapsedSeconds = getCurrentElapsedSeconds();
    }

    public int getFinishGoldAwarded() {
        return finishGoldAwarded;
    }

    public int getFinishDiamondAwarded() {
        return finishDiamondAwarded;
    }
}
