package com.closetruth.pet;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pets")
public class PetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // 宠物类型：cat, dog, dragon, etc.

    @Column(nullable = false)
    private int level = 1;

    @Column(nullable = false)
    private int experience = 0;

    @Column(nullable = false)
    private int happiness = 50; // 0-100

    @Column(nullable = false)
    private int hunger = 50; // 0-100, 越低越饿

    @Column(nullable = false)
    private int energy = 100; // 0-100

    @Column(nullable = false)
    private long lastFedAt;

    @Column(nullable = false)
    private long lastPlayedAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastActiveAt;

    protected PetEntity() {
    }

    public PetEntity(String name, String type) {
        this.name = name;
        this.type = type;
        this.createdAt = Instant.now();
        this.lastActiveAt = Instant.now();
        long now = System.currentTimeMillis();
        this.lastFedAt = now;
        this.lastPlayedAt = now;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    public int getHappiness() {
        return happiness;
    }

    public void setHappiness(int happiness) {
        this.happiness = Math.max(0, Math.min(100, happiness));
    }

    public int getHunger() {
        return hunger;
    }

    public void setHunger(int hunger) {
        this.hunger = Math.max(0, Math.min(100, hunger));
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(100, energy));
    }

    public long getLastFedAt() {
        return lastFedAt;
    }

    public void setLastFedAt(long lastFedAt) {
        this.lastFedAt = lastFedAt;
    }

    public long getLastPlayedAt() {
        return lastPlayedAt;
    }

    public void setLastPlayedAt(long lastPlayedAt) {
        this.lastPlayedAt = lastPlayedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    // Game logic methods
    public void feed() {
        this.hunger = Math.min(100, this.hunger + 30);
        this.energy = Math.min(100, this.energy + 10);
        this.happiness = Math.min(100, this.happiness + 5);
        this.lastFedAt = System.currentTimeMillis();
        this.lastActiveAt = Instant.now();
    }

    public void play() {
        if (this.energy >= 20 && this.hunger >= 10) {
            this.happiness = Math.min(100, this.happiness + 15);
            this.energy -= 20;
            this.hunger -= 10;
            this.experience += 10;
            this.lastPlayedAt = System.currentTimeMillis();
            this.lastActiveAt = Instant.now();
            checkLevelUp();
        }
    }

    public void train() {
        if (this.energy >= 30 && this.hunger >= 20) {
            this.experience += 25;
            this.energy -= 30;
            this.hunger -= 20;
            this.happiness = Math.max(0, this.happiness - 5);
            this.lastActiveAt = Instant.now();
            checkLevelUp();
        }
    }

    private void checkLevelUp() {
        int expNeeded = this.level * 100;
        if (this.experience >= expNeeded) {
            this.level++;
            this.experience -= expNeeded;
            this.happiness = Math.min(100, this.happiness + 20);
            this.energy = 100;
        }
    }

    public void rest() {
        this.energy = Math.min(100, this.energy + 50);
        this.hunger = Math.max(0, this.hunger - 10);
        this.lastActiveAt = Instant.now();
    }

    public void updatePassiveStats() {
        long now = System.currentTimeMillis();
        long hoursSinceFed = (now - this.lastFedAt) / (1000 * 60 * 60);
        long hoursSincePlayed = (now - this.lastPlayedAt) / (1000 * 60 * 60);

        // Hunger decreases over time
        this.hunger = Math.max(0, this.hunger - (int) (hoursSinceFed * 5));
        
        // Happiness decreases if not played with
        this.happiness = Math.max(0, this.happiness - (int) (hoursSincePlayed * 3));
        
        // Energy slowly recovers
        this.energy = Math.min(100, this.energy + (int) (hoursSinceFed * 2));
    }

    public int getExpToNextLevel() {
        return this.level * 100;
    }

    public double getExpProgress() {
        return (double) this.experience / getExpToNextLevel();
    }
}
