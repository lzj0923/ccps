package com.ccps.backend.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ccps.backend.web.DownloadContentDisposition;

class ContractDownloadDispositionTest {
    @Test
    void usesRfc5987InsteadOfMimeEncodedWordsForChineseFileNames() {
        String header = DownloadContentDisposition.attachment("张-202-租赁合同.pdf");

        assertTrue(header.startsWith("attachment; filename=\"tenancy-agreement.pdf\"; filename*=UTF-8''"));
        assertTrue(header.contains("%E5%BC%A0-202-%E7%A7%9F%E8%B5%81%E5%90%88%E5%90%8C.pdf"));
        assertFalse(header.contains("=?UTF-8?Q?"));
    }

    @Test
    void keepsTheRealExtensionForPaymentProofDownloads() {
        String header = DownloadContentDisposition.attachment("租金付款凭证.jpg");

        assertTrue(header.contains("filename=\"download.jpg\""));
        assertTrue(header.endsWith("%E7%A7%9F%E9%87%91%E4%BB%98%E6%AC%BE%E5%87%AD%E8%AF%81.jpg"));
    }
}
