package org.example.devpg.domian.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long amount;
    private String orderId;
    private LocalDateTime settledAt = LocalDateTime.now();

    public Settlement(Long amount, String orderId) {
        this.amount = amount; this.orderId = orderId;
    }
}
