package com.ccps.backend.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public interface AccountingSyncGateway {
    Path export(String batchNo, List<Record> records) throws IOException;

    record Record(Long sourceId, String transactionNo, String documentType, LocalDate documentDate,
                  String partyCode, String partyName, String projectCode, String projectName,
                  String unitNo, String description, BigDecimal amount, String currency,
                  String paymentMethod) {
    }
}
