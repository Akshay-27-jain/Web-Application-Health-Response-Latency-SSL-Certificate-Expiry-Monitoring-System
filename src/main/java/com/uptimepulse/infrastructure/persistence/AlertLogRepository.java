package com.uptimepulse.infrastructure.persistence;

import com.uptimepulse.domain.model.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
    List<AlertLog> findByUserIdOrderBySentAtDesc(Long userId);
}
