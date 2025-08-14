package com.potential_radar.PR.user.service;

import com.potential_radar.PR.user.model.EmailVerification;
import com.potential_radar.PR.user.model.Provider;
import com.potential_radar.PR.user.model.User;
import com.potential_radar.PR.user.repository.EmailVerificationRepository;
import com.potential_radar.PR.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailAuthService {
    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${email.verification.expiration-minutes:3}")
    private long expirationMinutes;

    public void validateAndSendCode(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getProvider() != Provider.EMAIL) {
                throw new IllegalArgumentException("이미 " + user.getProvider().name() + " 계정으로 가입된 이메일입니다. 소셜 로그인을 이용해주세요.");
            } else {
                throw new IllegalArgumentException("이미 가입된 이메일입니다.");
            }
        });

        sendCodeAsync(email);
    }


    @Async
    @Transactional
    public void sendCodeAsync(String email) {
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        emailVerificationRepository.deleteByEmail(email);
        emailVerificationRepository.save(new EmailVerification(email, code));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[PR] 이메일 인증번호");
        message.setText("인증번호는 " + code + " 입니다. (" + expirationMinutes + "분 이내 입력)");

        mailSender.send(message);
    }

    @Transactional
    public boolean verifyCode(String email, String inputCode) {
        Optional<EmailVerification> verificationOpt = emailVerificationRepository.findByEmail(email);

        if (verificationOpt.isEmpty()) {
            return false; // 해당 이메일로 요청된 코드가 없음
        }

        EmailVerification verification = verificationOpt.get();
        boolean isExpired = verification.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(expirationMinutes));

        if (isExpired) {
            emailVerificationRepository.delete(verification); // 만료된 코드는 삭제
            return false; // 만료되었으므로 실패
        }

        boolean isCorrect = verification.getCode().equals(inputCode);
        if (isCorrect) {
            emailVerificationRepository.delete(verification); // 인증 성공 시 일회용으로 사용되도록 삭제
        }
        return isCorrect;
    }

}
