package com.potential_radar.PR.config;

import com.potential_radar.PR.user.domain.User;
import com.potential_radar.PR.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitialDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // DB에 사용자가 한 명도 없을 때만 초기 데이터 생성
        if (userRepository.count() == 0) {
            log.info("데이터베이스가 비어있어 초기 사용자 데이터를 생성합니다.");

            User user1 = User.builder()
                    .email("leader@test.com")
                    .password(passwordEncoder.encode("password123!"))
                    .name("김리더")
                    .nickname("친절한리더")
                    .isPortfolioOpen(true)
                    .provider(User.Provider.LOCAL)
                    .reputationScore(new BigDecimal("4.5"))
                    .reviewCount(10)
                    .build();

            User user2 = User.builder()
                    .email("backend@test.com")
                    .password(passwordEncoder.encode("password123!"))
                    .name("박백엔드")
                    .nickname("자바고수")
                    .isPortfolioOpen(true)
                    .provider(User.Provider.LOCAL)
                    .reputationScore(new BigDecimal("4.8"))
                    .reviewCount(25)
                    .build();

            User user3 = User.builder()
                    .email("frontend@test.com")
                    .password(passwordEncoder.encode("password123!"))
                    .name("이프론트")
                    .nickname("리액트신")
                    .isPortfolioOpen(false)
                    .provider(User.Provider.LOCAL)
                    .reputationScore(new BigDecimal("4.2"))
                    .reviewCount(5)
                    .build();

            userRepository.saveAll(List.of(user1, user2, user3));
            log.info("초기 사용자 데이터 생성이 완료되었습니다.");
        } else {
            log.info("데이터베이스에 이미 사용자가 존재하므로 초기 데이터 생성을 건너뜁니다.");
        }
    }
}
