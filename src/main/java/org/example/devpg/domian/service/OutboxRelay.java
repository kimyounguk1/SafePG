package org.example.devpg.domian.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.devpg.domian.entity.Outbox;
import org.example.devpg.domian.repository.OutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void publishEvents() {
        List<Outbox> outboxes = outboxRepository.findAllByProcessedFalse();

        for (Outbox outbox : outboxes) {
            try{
                kafkaTemplate.send(outbox.getEventType(), outbox.getPayload())
                        .get(3, TimeUnit.SECONDS);

                outbox.markProcessed();
            } catch (Exception e) {
                log.error("Kafka 전송 실패, 다음 스케줄에 재시도합니다. ID: {}", outbox.getId());
            }
        }
    }

}
