package com.closetruth.season;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "week_claim")
public class WeekClaimEntity {

    @Id
    @Column(length = 32, nullable = false)
    private String weekKey;

    @Column(nullable = false)
    private Instant claimedAt;

    @Column(nullable = false)
    private int goldGranted;

    protected WeekClaimEntity() {
    }

    public WeekClaimEntity(String weekKey, Instant claimedAt, int goldGranted) {
        this.weekKey = weekKey;
        this.claimedAt = claimedAt;
        this.goldGranted = goldGranted;
    }

    public String getWeekKey() {
        return weekKey;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public int getGoldGranted() {
        return goldGranted;
    }
}
