package com.ccps.backend.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileAccountingSyncGateway implements AccountingSyncGateway {
    private final Path exportRoot;

    public FileAccountingSyncGateway(@Value("${ccps.storage.accounting-exports:uploads/accounting-exports}") String root) {
        this.exportRoot = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public Path export(String batchNo, List<Record> records) throws IOException {
        Files.createDirectories(exportRoot);
        Path target = exportRoot.resolve(batchNo + ".csv").normalize();
        if (!target.startsWith(exportRoot)) throw new IOException("Invalid accounting export path");
        List<String> lines = new ArrayList<>();
        // UTF-8 BOM keeps Chinese customer/project names readable when the
        // import file is opened by common Windows accounting tools or Excel.
        lines.add("\uFEFFDocumentNo,DocumentDate,DocumentType,CustomerCode,CustomerName,ProjectCode,ProjectName,UnitNo,Description,Amount,Currency,PaymentMethod,SourceId");
        for (Record row : records) {
            lines.add(String.join(",", csv(row.transactionNo()), csv(row.documentDate()), csv(row.documentType()),
                    csv(row.partyCode()), csv(row.partyName()), csv(row.projectCode()), csv(row.projectName()),
                    csv(row.unitNo()), csv(row.description()), csv(row.amount()), csv(row.currency()),
                    csv(row.paymentMethod()), csv(row.sourceId())));
        }
        Files.write(target, lines, StandardCharsets.UTF_8);
        return target;
    }

    public Path locate(String batchNo) {
        Path target = exportRoot.resolve(batchNo + ".csv").normalize();
        if (!target.startsWith(exportRoot)) throw new IllegalArgumentException("Invalid accounting export path");
        return target;
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
