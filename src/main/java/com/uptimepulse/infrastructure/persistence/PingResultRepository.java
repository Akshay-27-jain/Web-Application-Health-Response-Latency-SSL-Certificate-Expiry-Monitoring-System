package com.uptimepulse.infrastructure.persistence;

import com.uptimepulse.domain.model.PingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PingResultRepository extends JpaRepository<PingResult, Long> {
    List<PingResult> findTop10ByMonitorIdOrderByTimestampDesc(Long monitorId);
    List<PingResult> findTop50ByMonitorIdOrderByTimestampDesc(Long monitorId);
}
