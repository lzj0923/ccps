package com.ccps.backend.compliance;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class StaticCompliancePagesTest {

    @Test
    void dataDeletionInstructionsArePublishedAsAStaticResource() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/static/data-deletion.html")) {
            assertNotNull(stream, "Meta data-deletion instructions page must exist");
            String html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(html.contains("CCPS Data Deletion Request"));
            assertTrue(html.contains("qq2290715152@gmail.com"));
            assertTrue(html.contains("30 天"));
            assertTrue(html.contains("User Data Deletion"));
        }
    }
}
