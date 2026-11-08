package io.github.sleep404nf.serverdock.server;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public List<MemberResponse> findAll(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable Long serverId) {
        return memberService.findAll(userId(jwt), serverId);
    }

    @PatchMapping("/{userId}")
    public MemberResponse changeRole(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable Long serverId,
                                     @PathVariable("userId") Long targetUserId,
                                     @Valid @RequestBody UpdateMemberRoleRequest request) {
        return memberService.changeRole(userId(jwt), serverId, targetUserId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt,
                       @PathVariable Long serverId,
                       @PathVariable("userId") Long targetUserId) {
        memberService.remove(userId(jwt), serverId, targetUserId);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
