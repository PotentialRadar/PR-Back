package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.model.EmailVerification;
import com.potential_radar.PR.user.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailAuthService {
    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public void sendVerificationCode(String email) {
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        emailVerificationRepository.deleteByEmail(email);
        emailVerificationRepository.save(new EmailVerification(email, code));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[PR] 이메일 인증번호");
        message.setText("인증번호는 " + code + " 입니다. (3분 이내 입력)");

        mailSender.send(message);
    }

    public boolean verifyCode(String email, String inputCode) {
        return emailVerificationRepository.findByEmail(email)
                .filter(v -> !v.isExpired())
                .map(v -> v.getCode().equals(inputCode))
                .orElse(false);
    }

}
