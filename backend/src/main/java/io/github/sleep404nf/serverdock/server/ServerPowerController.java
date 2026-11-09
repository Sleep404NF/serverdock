package io.github.sleep404nf.serverdock.server;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/servers/{serverId}")
@RequiredArgsConstructor
public class ServerPowerController {

    private final ServerPowerService serverPowerService;

    @PostMapping("/start")
    public ServerStatusResponse start(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable Long serverId) {
        return serverPowerService.start(userId(jwt), serverId);
    }

    @PostMapping("/stop")
    public ServerStatusResponse stop(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable Long serverId) {
        return serverPowerService.stop(userId(jwt), serverId);
    }

    @GetMapping("/status")
    public ServerStatusResponse status(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long serverId) {
        return serverPowerService.status(userId(jwt), serverId);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}