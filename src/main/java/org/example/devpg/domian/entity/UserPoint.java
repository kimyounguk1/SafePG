package org.example.devpg.domian.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.devpg.global.exception.InsufficientBalanceException;

@Entity
@Getter
@NoArgsConstructor
public class UserPoint {

    @Id
    private Long userId;

    private Long balance;

    public void deduct(Long amount) {
        if(this.balance < amount) throw new InsufficientBalanceException("잔액 부족");
        this.balance -= amount;
    }

    public void add(Long amount) {
        this.balance += amount;
    }

}
