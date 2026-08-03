package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

class ContractTemplatePdfServiceBeanTest {
    @Test
    void contractTemplateServiceIsRegisteredAsSpringBean() {
        assertNotNull(ContractTemplatePdfService.class.getAnnotation(Service.class));
    }
}
