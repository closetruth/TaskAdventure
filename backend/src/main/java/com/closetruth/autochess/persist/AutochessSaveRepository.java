package com.closetruth.autochess.persist;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AutochessSaveRepository extends JpaRepository<AutochessSaveEntity, Long> {
}
