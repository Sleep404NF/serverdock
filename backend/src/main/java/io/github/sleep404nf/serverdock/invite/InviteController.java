package io.github.sleep404nf.serverdock.invite;

import io.github.sleep404nf.serverdock.server.ServerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InviteController {

    private final InviteService inviteService;

    @PostMapping("/api/servers/{serverId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse create(@AuthenticationPrincipal Jwt jwt,
                                 @PathVariable Long serverId,
                                 @Valid @RequestBody CreateInviteRequest request) {
        return inviteService.create(userId(jwt), serverId, request);
    }

    @GetMapping("/api/servers/{serverId}/invites")
    public List<InviteResponse> findAll(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable Long serverId) {
        return inviteService.findAll(userId(jwt), serverId);
    }

    @DeleteMapping("/api/servers/{serverId}/invites/{inviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Jwt jwt,
                       @PathVariable Long serverId,
                       @PathVariable Long inviteId) {
        inviteService.revoke(userId(jwt), serverId, inviteId);
    }

    @PostMapping("/api/invites/{code}/accept")
    public ServerResponse accept(@AuthenticationPrincipal Jwt jwt,
                                 @PathVariable String code) {
        return inviteService.accept(userId(jwt), code);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
