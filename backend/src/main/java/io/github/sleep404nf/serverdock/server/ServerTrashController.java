package io.github.sleep404nf.serverdock.server;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 서버 삭제(DELETE /api/servers/{id})는 ServerController에 있고, 휴지통으로 옮긴다.
 */
@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerTrashController {

    private final ServerTrashService serverTrashService;

    @GetMapping("/trash")
    public List<TrashedServerResponse> findTrash(@AuthenticationPrincipal Jwt jwt) {
        return serverTrashService.findTrash(userId(jwt));
    }

    @PostMapping("/{serverId}/restore")
    public ServerResponse restore(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable Long serverId) {
        return serverTrashService.restore(userId(jwt), serverId);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
