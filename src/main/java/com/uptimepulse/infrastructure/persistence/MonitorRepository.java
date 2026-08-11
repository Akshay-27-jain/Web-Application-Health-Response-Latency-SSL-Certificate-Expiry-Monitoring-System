package com.uptimepulse.infrastructure.persistence;

import com.uptimepulse.domain.model.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long> {
    List<Monitor> findByUserId(Long userId);
    Optional<Monitor> findByPublicId(String publicId);
}
