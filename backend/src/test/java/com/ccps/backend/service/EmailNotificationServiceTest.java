package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ccps.backend.dto.EmailBindingRequest;
import com.ccps.backend.dto.EmailVerificationRequest;
import com.ccps.backend.dto.OwnerNotificationResponse.Channel;
import com.ccps.backend.mapper.OwnerNotificationMapper;
import com.ccps.backend.mapper.OwnerNotificationMapper.EmailSubscriptionRow;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {
    @Mock private OwnerNotificationMapper mapper;
    @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock private JavaMailSender mailSender;
    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        service = new EmailNotificationService(
                mapper, mailSenderProvider, "smtp.example.com", "notice@example.com", "CCPS");
    }

    @Test
    void sendsVerificationCodeAndStoresOnlyItsHash() {
        Channel result = service.requestVerification(42L, new EmailBindingRequest("Owner@Example.com"));

        verify(mapper).saveEmailVerification(org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq("owner@example.com"), anyString());
        verify(mailSender).send(any(SimpleMailMessage.class));
        assertThat(result.status()).isEqualTo("pending");
        assertThat(result.destination()).isEqualTo("owner@example.com");
    }

    @Test
    void verifiesCodeAndEnablesEmailSubscription() {
        EmailSubscriptionRow row = new EmailSubscriptionRow();
        row.setDestination("owner@example.com");
        row.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(5));
        row.setVerificationCodeHash(new BCryptPasswordEncoder().encode("123456"));
        when(mapper.findEmailSubscription(42L)).thenReturn(row);

        Channel result = service.verify(42L, new EmailVerificationRequest("owner@example.com", "123456"));

        verify(mapper).verifyEmailSubscription(42L, "owner@example.com");
        verify(mailSender).send(any(SimpleMailMessage.class));
        assertThat(result.status()).isEqualTo("enabled");
        assertThat(result.verified()).isTrue();
    }
}
