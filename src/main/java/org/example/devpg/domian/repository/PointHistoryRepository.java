package org.example.devpg.domian.repository;

import org.example.devpg.domian.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    boolean existsByPaymentId(Long paymentId);
}