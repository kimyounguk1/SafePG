package org.example.devpg.domian.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.devpg.domian.entity.*;
import org.example.devpg.domian.repository.*;
import org.example.devpg.global.exception.InsufficientBalanceException;
import org.example.devpg.global.mock.MockPgClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointConsumer {

    private final UserPointRepository pointRepository;
    private final PointHistoryRepository historyRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SettlementRepository settlementRepository;
    private final PaymentRepository paymentRepository;
    private final MockPgClient pgClient;
    private final OutboxRepository outboxRepository;

    //잔액 부족 에러는 재시도 하지 않음
    @RetryableTopic(attempts = "3", exclude = InsufficientBalanceException.class)
    @KafkaListener(topics = "payment.success")
    @Transactional
    public void deductPoint(String message) throws Exception {

        PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);

        UserPoint point = pointRepository.findByUserIdWithLock(event.getUserId()).orElseThrow();

        //이미 차감된 history에 기록된 것은 중복 방지
        if (historyRepository.existsByPaymentId(event.getPaymentId())) return;

        try {
            // 차감 및 이력 저장
            point.deduct(event.getAmount());
            historyRepository.save(new PointHistory(event.getPaymentId(), event.getUserId(), event.getAmount()));

            // 성공 시 정산으로 넘김
            outboxRepository.save(new Outbox("settlement.process", objectMapper.writeValueAsString(event)));

        } catch (InsufficientBalanceException e) {
            // 포인트 부족 시: 이미 성공한 PG 결제를 취소하기 위해 이벤트 발행 (Saga Pattern)
            outboxRepository.save(new Outbox("payment.cancel", objectMapper.writeValueAsString(event)));
        }
    }

    @KafkaListener(topics = "settlement.process")
    @Transactional
    public void processSettlement(String message) throws Exception {
        PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
        settlementRepository.save(new Settlement(event.getAmount(), event.getOrderId()));
    }

    @KafkaListener(topics = "payment.cancel")
    @Transactional
    public  void cancelPayment(String message) throws Exception {
        PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
        Payment payment = paymentRepository.findById(event.getPaymentId()).orElseThrow();
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            pgClient.cancel(payment.getExternalId());
            payment.makeCancelled();
        }
    }


}
