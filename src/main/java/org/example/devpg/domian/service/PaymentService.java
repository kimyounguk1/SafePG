package org.example.devpg.domian.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void startPayment(Long userId, String orderId, Long amount) throws JsonProcessingException {
        Payment payment = paymentRepository.save(new Payment(userId, orderId, amount));

        PaymentEvent event = new PaymentEvent(payment.getId(), userId, amount, orderId);
        String payload = objectMapper.writeValueAsString(event);

        outboxRepository.save(new Outbox("payment.requested", payload));

    }


}
