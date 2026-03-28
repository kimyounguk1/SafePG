package org.example.devpg.domian.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String orderId;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String externalId; //PG사 승인 번호

    private LocalDateTime updatedAt;

    public Payment(Long userId, String orderId, Long amount) {
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.INIT;
        this.updatedAt = LocalDateTime.now();
    }

    public void makeSuccess(String externalId) {
        this.status = PaymentStatus.SUCCESS;
        this.externalId = externalId;
        this.updatedAt = LocalDateTime.now();
    }

    public void makeFail() {
        this.status = PaymentStatus.FAIL;
        this.updatedAt = LocalDateTime.now();
    }

    public void makeCancelled() {
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

}
