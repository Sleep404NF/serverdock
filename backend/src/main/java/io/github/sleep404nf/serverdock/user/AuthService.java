package io.github.sleep404nf.serverdock.user;

import io.github.sleep404nf.serverdock.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /**
     * 회원가입. 첫 가입자는 ADMIN, 그다음부터는 USER가 된다.
     */
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }

        SystemRole role = userRepository.count() == 0 ? SystemRole.ADMIN : SystemRole.USER;
        User user = User.create(request.username(), passwordEncoder.encode(request.password()), role);

        return UserResponse.from(userRepository.save(user));
    }

    /**
     * 로그인. 아이디가 없는 경우와 비밀번호가 틀린 경우를 같은 메시지로 응답한다.
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        return tokenService.issue(user);
    }

    /**
     * 토큰의 사용자 id로 내 정보를 조회한다.
     */
    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
    }
}
