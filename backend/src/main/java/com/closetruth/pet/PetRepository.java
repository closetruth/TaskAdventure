package com.closetruth.pet;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PetRepository extends JpaRepository<PetEntity, Long> {
    List<PetEntity> findAllByOrderByCreatedAtDesc();
}
