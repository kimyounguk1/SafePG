# SafePG

## 프로젝트 개요

단순한 결제 연동을 넘어, 네트워크 장애와 분산 환경에서도 데이터 결함이 없는 결제 로직 구현

## 주요 기능

결제 승인 및 취소 : 외부 PG사(Mock) API 연동을 통한 실시간 결제 처리
이벤트 기반 포인트 차감 : 결제 성공 시 카프카 이벤트를 발행하여 비동기적인 포인트 시스템과 연동
보상 트랜잭션(Saga) : 포인트 차감 실패 시 이미 성공한 결제를 자동으로 취소하여 정합성 유지
자동 재시도 로직 : RetryableTopic을 활용하여 일시적인 네트워크 오류 발생 시 자동 복구 시도

## 핵심 기술

**Transactional Outbox Pattern 적용**

이유
- DB 업테이트와 Kafka 메시지 발송이 서로 다른 트랜잭션으로 관리되어, 하나는 성공하고 하나는 실패하는 이중 쓰기 문제 발생

해결
- 메시지를 즉시 발송하지 않고 동일한 트랜잭션 내에서 Outbox 테이블에 저장, 별도의 Relay스케줄러가 이를 읽어 발행함으로써 메시지 발행의 원자성 확보
```java
@Transactional
    public void startPayment(Long userId, String orderId, Long amount) throws JsonProcessingException {
        Payment payment = paymentRepository.save(new Payment(userId, orderId, amount));

        PaymentEvent event = new PaymentEvent(payment.getId(), userId, amount, orderId);
        String payload = objectMapper.writeValueAsString(event);

        outboxRepository.save(new Outbox("payment.requested", payload));

    }
```

**멱등성 보장**

이유
- 카프카의 최소 한번 전달(At-Least-Once) 정책으로 인해 동일한 결제 완료 이벤트가 중복 수신되어 포인트가 이중차감될 위험이 존재

해결
- PointHistory 테이블에 paymentId 유니크 제약 조건을 설정하고, 로직 진입 전 처리 여부를 확인하는 로직을 배치하여 중복 처리 차단
```java
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
```

**외부 API 호출의 정합성 유지**

이유 
- 내 시스템의 DB 상태 변경과 외부 PG사 API 호출 사이의 간극으로 인한 존비 데이터 발생 가능성이 존재

해결
- 우리 시스템의 고유 ID를 PG사에 전달하여 PG사 레벨의 멱등성 활용

## 트러블 슈팅

**DB 커밋 전 메시지 선도착으로 인한 Race Condition**

현상
- 카프카 메시지가 DB커밋보다 빠르게 컨슈머에 도착하여, 컨슈머가 DB 조회시 데이터를 찾지 못하거나, 이전 상태를 조회

원인
- DB 격리 수준으로 인해 커밋 전에는 데이터 변경이 보이지 않으나, 비동기 메시지는 즉시 전송되기 때문임

해결
- Outbox 패턴을 도입하여 메시지 발행 시점을 DB 커밋 이후로 강제함. 이를 통해 컨슈머가 항상 최신 상태의 데이터를 조회할 수 있도록 보장

**PG API 타임아웃 시의 상태 미확정 문제**

현상
- PG 승인 요청을 보냈으나 네트워크 타임아웃으로 응답을 받지 못한 경우, 결제 성공 여부를 알 수가 없어 DB 상태를 업데이트 하지 못함

원인
- 분산 환경에서의 피할 수 없는 네트워크 문제

해결
- 스케줄러를 통해 이후 PG사의 조회를 호출하여 실제 결제 여부를 사루 대조하여 상태 동기화

결제 요청은 갔지만 payment의 status는 INIT으로 유지됨
```java
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
```

주기적으로 INIT상태를 유지하며 생성된지 10분이 지난 payment를 조회하여 pg사의 조회 기능을 사용해 상태 동기화
```java
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
```
