package com.ccps.backend.controller;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.service.SignatureWhatsAppService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/e-signatures/requests/{requestId}/whatsapp")
public class AdminSignatureWhatsAppController {
    private final SignatureWhatsAppService service;
    public AdminSignatureWhatsAppController(SignatureWhatsAppService service){this.service=service;}
    public record SendRequest(@NotBlank @Size(max=200) String token, @Size(max=40) String recipientPhone) {}
    @GetMapping
    public SignatureWhatsAppService.State state(@PathVariable("requestId") Long requestId){return service.state(requestId);}
    @PostMapping
    public SignatureWhatsAppService.State send(@PathVariable("requestId") Long requestId,@Valid @RequestBody SendRequest payload,HttpServletRequest request){
        return service.send(AuthInterceptor.userId(request),requestId,payload.token(),payload.recipientPhone());
    }
}
