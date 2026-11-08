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
}
