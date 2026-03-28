package org.example.devpg.domian.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.devpg.domian.dto.PgResponse;
import org.example.devpg.domian.entity.Outbox;
import org.example.devpg.domian.entity.Payment;
import org.example.devpg.domian.entity.PaymentEvent;
import org.example.devpg.domian.entity.PaymentStatus;
import org.example.devpg.domian.repository.OutboxRepository;
import org.example.devpg.domian.repository.PaymentRepository;
import org.example.devpg.global.mock.MockPgClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentRepository paymentRepository;
    private final MockPgClient pgClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2000))
    @KafkaListener(topics = "payment.requested")
    @Transactional
    //해당 메서드에서 발생할 수 있는 좀비는 카프카에 send 성공 후 db상태 변경 실패(INIT)
    public void handlePgApproval(String message) throws Exception {
        PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);

        Payment payment = paymentRepository.findByIdWithLock(event.getPaymentId()).orElseThrow();
        if(payment.getStatus() != PaymentStatus.INIT) return; //상태가 INIT이 아니면 리턴(중복 방지)

        try{
            PgResponse res = pgClient.authorize(payment.getOrderId(), payment.getAmount());
            if("SUCCESS".equals(res.getCode())){
                //외부 결제 성공으로 변경
                payment.makeSuccess(res.getPgReceiptId());
                outboxRepository.save(new Outbox("payment.success", objectMapper.writeValueAsString(event)));
            } else {
                payment.makeFail(); //실패 처리
            }
        } catch (Exception e) {
            throw new RuntimeException("PG 네트워크 오류, 재시도합니다.");
        }
    }
}
