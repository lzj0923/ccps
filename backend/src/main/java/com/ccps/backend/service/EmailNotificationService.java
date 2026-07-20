package com.ccps.backend.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.EmailBindingRequest;
import com.ccps.backend.dto.EmailSubscriptionToggleRequest;
import com.ccps.backend.dto.EmailVerificationRequest;
import com.ccps.backend.dto.OwnerNotificationResponse.Channel;
import com.ccps.backend.mapper.OwnerNotificationMapper;
import com.ccps.backend.mapper.OwnerNotificationMapper.EmailDeliveryRow;
import com.ccps.backend.mapper.OwnerNotificationMapper.EmailSubscriptionRow;

@Service
public class EmailNotificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private final OwnerNotificationMapper mapper;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder codeEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final String mailHost;
    private final String fromAddress;
    private final String senderName;

    public EmailNotificationService(
            OwnerNotificationMapper mapper,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${ccps.mail.from:}") String fromAddress,
            @Value("${ccps.mail.sender-name:CCPS 家慶佳業}") String senderName) {
        this.mapper = mapper;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.mailHost = mailHost;
        this.fromAddress = fromAddress;
        this.senderName = senderName;
    }

    @Transactional
    public Channel requestVerification(Long userId, EmailBindingRequest request) {
        requireMailConfiguration();
        String email = request.email().trim().toLowerCase();
        EmailSubscriptionRow current = mapper.findEmailSubscription(userId);
        if (current != null && current.getLastVerificationSentAt() != null
                && current.getLastVerificationSentAt().isAfter(LocalDateTime.now().minusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait one minute before requesting another verification code");
        }

        String code = String.valueOf(100000 + random.nextInt(900000));
        mapper.saveEmailVerification(userId, email, codeEncoder.encode(code));
        send(email, "CCPS 郵件訂閱驗證碼",
                "您的 CCPS 郵件驗證碼是：" + code + "\n\n驗證碼 10 分鐘內有效。若非本人操作，請忽略此郵件。");
        return new Channel("email", "郵件", "pending", email, false);
    }

    @Transactional
    public Channel verify(Long userId, EmailVerificationRequest request) {
        requireMailConfiguration();
        String email = request.email().trim().toLowerCase();
        EmailSubscriptionRow subscription = mapper.findEmailSubscription(userId);
        if (subscription == null || !email.equalsIgnoreCase(subscription.getDestination())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email verification request not found");
        }
        if (subscription.getVerificationExpiresAt() == null
                || subscription.getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email verification code has expired");
        }
        if (subscription.getVerificationCodeHash() == null
                || !codeEncoder.matches(request.code(), subscription.getVerificationCodeHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect email verification code");
        }
        mapper.verifyEmailSubscription(userId, email);
        send(email, "CCPS 郵件通知已啟用",
                "您的郵件地址已成功綁定。之後的重要房款、租金、預備金及維修通知會發送到此郵箱。");
        return new Channel("email", "郵件", "enabled", email, true);
    }

    @Transactional
    public Channel toggle(Long userId, EmailSubscriptionToggleRequest request) {
        if (mapper.toggleEmailSubscription(userId, request.enabled()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Please verify an email address first");
        }
        EmailSubscriptionRow subscription = mapper.findEmailSubscription(userId);
        return channel(subscription);
    }

    public Channel channel(EmailSubscriptionRow subscription) {
        if (subscription == null) return new Channel("email", "郵件", "unbound", null, false);
        boolean verified = subscription.getVerifiedAt() != null;
        String status = !verified ? "pending" : Boolean.TRUE.equals(subscription.getEnabled()) ? "enabled" : "disabled";
        return new Channel("email", "郵件", status, subscription.getDestination(), verified);
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 60000)
    public void deliverPendingNotifications() {
        if (!mailConfigured()) return;
        for (EmailDeliveryRow delivery : mapper.findPendingEmailDeliveries()) {
            mapper.claimEmailDelivery(delivery.getNotificationId(), delivery.getDestination());
            try {
                send(delivery.getDestination(), "CCPS 通知：" + delivery.getTitle(), delivery.getBody());
                mapper.markEmailDeliverySent(delivery.getNotificationId());
            } catch (RuntimeException ex) {
                mapper.markEmailDeliveryFailed(delivery.getNotificationId(), ex.getMessage());
                log.warn("Email notification {} failed: {}", delivery.getNotificationId(), ex.getMessage());
            }
        }
    }

    private void send(String destination, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderName + " <" + fromAddress + ">");
        message.setTo(destination);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private void requireMailConfiguration() {
        if (!mailConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email service is not configured");
        }
    }

    private boolean mailConfigured() {
        return mailSender != null && mailHost != null && !mailHost.isBlank()
                && fromAddress != null && !fromAddress.isBlank();
    }
}
