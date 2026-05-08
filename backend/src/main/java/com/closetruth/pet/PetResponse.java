package com.closetruth.pet;

import java.time.Instant;

public record PetResponse(
        Long id,
        String name,
        String type,
        int level,
        int experience,
        int expToNextLevel,
        double expProgress,
        int happiness,
        int hunger,
        int energy,
        long lastFedAt,
        long lastPlayedAt,
        Instant createdAt,
        Instant lastActiveAt,
        String status
) {
    public static PetResponse from(PetEntity pet) {
        pet.updatePassiveStats();
        
        String status = "happy";
        if (pet.getHunger() < 20) {
            status = "hungry";
        } else if (pet.getEnergy() < 20) {
            status = "tired";
        } else if (pet.getHappiness() < 30) {
            status = "sad";
        }
        
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getType(),
                pet.getLevel(),
                pet.getExperience(),
                pet.getExpToNextLevel(),
                pet.getExpProgress(),
                pet.getHappiness(),
                pet.getHunger(),
                pet.getEnergy(),
                pet.getLastFedAt(),
                pet.getLastPlayedAt(),
                pet.getCreatedAt(),
                pet.getLastActiveAt(),
                status
        );
    }
}
