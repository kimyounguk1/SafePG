package org.example.devpg.domian.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PgResponse {
    private String code; // SUCCESS, FAIL
    private String pgReceiptId;
    private String message;
}
