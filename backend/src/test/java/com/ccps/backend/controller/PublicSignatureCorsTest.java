package com.ccps.backend.controller;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.MediaType;
import com.ccps.backend.config.WebConfig;
import com.ccps.backend.service.ElectronicSignatureService;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PublicSignatureCorsTest {
    static final String ORIGIN="https://lzj.mydream.tw";
    static final String PATH="/api/public/signatures/invalid-diagnostic-token/sign";
    static class Registry extends CorsRegistry { Map<String,CorsConfiguration> mappings(){return getCorsConfigurations();} }
    MockMvc mvc;
    ElectronicSignatureService service;
    Registry registry;
    @BeforeEach void setup(){
        registry=new Registry();new WebConfig(null,null).addCorsMappings(registry);
        service=mock(ElectronicSignatureService.class);
        mvc=MockMvcBuilders.standaloneSetup(new PublicElectronicSignatureController(service))
            .setCustomHandlerMapping(()->{var mapping=new RequestMappingHandlerMapping();mapping.setCorsConfigurations(registry.mappings());return mapping;}).build();
    }
    @Test void productionOriginReachesPayloadValidationInsteadOf403() throws Exception {
        mvc.perform(post(PATH).header("Origin",ORIGIN).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(header().string("Access-Control-Allow-Origin",ORIGIN));
        verifyNoInteractions(service);
    }
    @Test void productionPreflightAccepted() throws Exception {
        var source=new UrlBasedCorsConfigurationSource();source.setCorsConfigurations(registry.mappings());
        var request=new MockHttpServletRequest("OPTIONS",PATH);
        request.addHeader("Origin",ORIGIN);request.addHeader("Access-Control-Request-Method","POST");request.addHeader("Access-Control-Request-Headers","content-type");
        var response=new org.springframework.mock.web.MockHttpServletResponse();
        assertThat(new org.springframework.web.cors.DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request),request,response)).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
        verifyNoInteractions(service);
    }
    @Test void allowedOriginCanReachMockSigningService() throws Exception {
        mvc.perform(post(PATH).header("Origin",ORIGIN).contentType(MediaType.APPLICATION_JSON)
            .content("{\"signerName\":\"Test\",\"signatureDataUrl\":\"data:image/png;base64,test-only\",\"consent\":true}"))
            .andExpect(status().isOk());
        verify(service).sign(eq("invalid-diagnostic-token"),any(),any(),nullable(String.class));
    }
    @Test void untrustedOriginsRemainRejected() throws Exception {
        for(String origin:new String[]{"https://evil.example","https://lzj.mydream.tw.evil.example","null"})
            mvc.perform(post(PATH).header("Origin",origin).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        verifyNoInteractions(service);
    }
    @Test void developmentAndNoOriginBehaviorPreserved() throws Exception {
        mvc.perform(post(PATH).header("Origin","http://localhost:5173").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void productionOriginAllowanceDoesNotExpandAdminCors(){
        var source=new UrlBasedCorsConfigurationSource();source.setCorsConfigurations(registry.mappings());
        var request=new MockHttpServletRequest("POST","/api/admin/example");
        assertThat(source.getCorsConfiguration(request).checkOrigin(ORIGIN)).isNull();
    }
}
