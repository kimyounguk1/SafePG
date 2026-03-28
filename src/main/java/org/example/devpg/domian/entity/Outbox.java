package org.example.devpg.domian.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String eventType;
    @Lob
    private String payload;

    private boolean processed = false;

    public Outbox(String eventType, String payload) {
        this.eventType = eventType;
        this.payload = payload;
    }
    public void markProcessed() { this.processed = true; }

}
