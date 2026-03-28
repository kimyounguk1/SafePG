package org.example.devpg.domian.batch;

import lombok.RequiredArgsConstructor;
import org.example.devpg.domian.dto.PgResponse;
import org.example.devpg.domian.entity.Payment;
import org.example.devpg.domian.repository.PaymentRepository;
import org.example.devpg.global.mock.MockPgClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReconciliationBatch {
    private final PaymentRepository paymentRepository;
    private final MockPgClient pgClient;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupZombies(){
        List<Payment> zombies = paymentRepository.findZombies(LocalDateTime.now().minusMinutes(10));

        for (Payment p : zombies) {
            PgResponse res = pgClient.checkStatus(p.getOrderId());
            if ("SUCCESS".equals(res.getCode())) p.makeSuccess(res.getPgReceiptId());
            else p.makeFail();
        }
    }
}
