package io.github.sleep404nf.serverdock.invite;

import io.github.sleep404nf.serverdock.common.BusinessException;
import io.github.sleep404nf.serverdock.server.Server;
import io.github.sleep404nf.serverdock.server.ServerAccessChecker;
import io.github.sleep404nf.serverdock.server.ServerMember;
import io.github.sleep404nf.serverdock.server.ServerMemberId;
import io.github.sleep404nf.serverdock.server.ServerMemberRepository;
import io.github.sleep404nf.serverdock.server.ServerResponse;
import io.github.sleep404nf.serverdock.server.ServerRole;
import io.github.sleep404nf.serverdock.user.User;
import io.github.sleep404nf.serverdock.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InviteService {

    private static final InviteExpiry DEFAULT_EXPIRY = InviteExpiry.ONE_DAY;
    private static final int CODE_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InviteRepository inviteRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final UserRepository userRepository;
    private final ServerAccessChecker serverAccessChecker;

    /**
     * 초대 링크 생성 - 어드민 이상만 가능. 어드민 초대는 오너만 가능.
     */
    @Transactional
    public InviteResponse create(Long userId, Long serverId, CreateInviteRequest request) {
        ServerMember me = serverAccessChecker.require(serverId, userId, ServerRole.ADMIN);

        if (request.role() == ServerRole.OWNER) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "OWNER로는 초대할 수 없습니다.");
        }

        if (request.role() == ServerRole.ADMIN && me.getRole() != ServerRole.OWNER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ADMIN 초대는 OWNER만 만들 수 있습니다.");
        }

        InviteExpiry expiry = request.expiresIn() != null ? request.expiresIn() : DEFAULT_EXPIRY;
        Invite invite = Invite.create(
                me.getServer(),
                generateCode(),
                request.role(),
                me.getUser(),
                LocalDateTime.now().plus(expiry.duration()),
                request.maxUses()
        );
        return InviteResponse.from(inviteRepository.save(invite));
    }

    /**
     * 서버 초대 링크 목록을 최근에 만든 것부터 리스트
     */
    public List<InviteResponse> findAll(Long userId, Long serverId) {
        serverAccessChecker.require(serverId, userId, ServerRole.ADMIN);

        return inviteRepository.findAllByServerIdWithCreator(serverId).stream()
                .map(InviteResponse::from)
                .toList();
    }

    /**
     * 초대 링크 폐기. 다른 서버 초대 id를 받아도 못 찾게 serverId까지 같이 확인.
     */
    @Transactional
    public void revoke(Long userId, Long serverId, Long inviteId) {
        serverAccessChecker.require(serverId, userId, ServerRole.ADMIN);

        Invite invite = inviteRepository.findByIdAndServerId(inviteId, serverId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "초대 링크를 찾을 수 없습니다."));
        inviteRepository.delete(invite);
    }

    /**
     * 초대 수락. 만료 혹은 횟수가 다 차면 410, 이미 멤버일 시 409
     */
    @Transactional
    public ServerResponse accept(Long userId, String code) {
        Invite invite = inviteRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "초대 링크를 찾을 수 없습니다."));

        if (invite.isExpired(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.GONE, "만료된 초대 링크입니다.");
        }

        Server server = invite.getServer();
        if (serverMemberRepository.existsById(new ServerMemberId(server.getId(), userId))) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 이 서버의 멤버입니다.");
        }

        if (inviteRepository.incrementUsedCount(invite.getId()) == 0) {
            throw new BusinessException(HttpStatus.GONE, "사용 횟수가 다 찬 초대 링크입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
        ServerMember member = serverMemberRepository.save(ServerMember.create(server, user, invite.getRole()));

        return ServerResponse.of(server, member.getRole());
    }

    /**
     * 16바이트 랜덤값을 URL에 넣을 수 있는 문자열로 변환.
     */
    private String generateCode() {
        byte[] bytes = new byte[CODE_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
