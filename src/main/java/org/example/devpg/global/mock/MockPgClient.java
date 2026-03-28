package org.example.devpg.global.mock;

import lombok.extern.slf4j.Slf4j;
import org.example.devpg.domian.dto.PgResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class MockPgClient {

    public PgResponse authorize(String orderId, Long amount) {
        if(Math.random() < 0.1 ) throw new RuntimeException("PG Timeout");
        return new PgResponse("SUCCESS", "PG-"+ UUID.randomUUID(), null);

    }

    public void cancel(String externalId) {
        log.info("PG 승인 취소 완료: {}", externalId);
    }

    public PgResponse checkStatus(String orderId) {
        return new PgResponse("SUCCESS", "PG-FIX", null);
    }

}
