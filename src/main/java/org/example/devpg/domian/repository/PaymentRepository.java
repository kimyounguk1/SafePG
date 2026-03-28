package org.example.devpg.domian.repository;

import jakarta.persistence.LockModeType;
import org.example.devpg.domian.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE) // 비관적 락
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdWithLock(Long id);

    @Query("select p from Payment p where p.status = 'INIT' and p.updatedAt < :time")
    List<Payment> findZombies(LocalDateTime time);

}
