package org.example.devpg.domian.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"paymentId"})})
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long paymentId;

    private Long userId;

    private Long amount;

    public PointHistory(Long paymentId, Long userId, Long amount) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.amount = amount;
    }

}
