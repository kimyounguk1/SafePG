package org.example.devpg.domian.repository;

import jakarta.persistence.LockModeType;
import org.example.devpg.domian.entity.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserPoint u where u.userId = :userId")
    Optional<UserPoint> findByUserIdWithLock(Long userId);
}
