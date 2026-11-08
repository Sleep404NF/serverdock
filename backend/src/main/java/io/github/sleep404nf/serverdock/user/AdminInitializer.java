package io.github.sleep404nf.serverdock.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시 관리자가 한 명도 없으면 설정값으로 관리자 계정을 만든다.
 * 관리자가 이미 있으면 아무것도 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class AdminInitializer implements ApplicationRunner {

    private static final String DEV_PASSWORD = "admin";

    private final AdminProperties adminProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsBySystemRole(SystemRole.ADMIN)) {
            return;
        }

        if (userRepository.existsByUsername(adminProperties.username())) {
            log.warn("아이디 '{}'를 일반 사용자가 이미 쓰고 있어 관리자를 만들지 않았습니다.", adminProperties.username());
            return;
        }

        User admin = User.create(
                adminProperties.username(),
                passwordEncoder.encode(adminProperties.password()),
                SystemRole.ADMIN
        );
        userRepository.save(admin);
        log.info("관리자 계정을 만들었습니다: {}", adminProperties.username());

        if (DEV_PASSWORD.equals(adminProperties.password())) {
            log.warn("개발용 기본 관리자 비밀번호를 쓰고 있습니다. 배포 전에 ADMIN_PASSWORD를 설정하세요.");
        }
    }
}
