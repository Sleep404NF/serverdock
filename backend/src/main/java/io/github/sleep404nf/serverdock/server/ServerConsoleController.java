package io.github.sleep404nf.serverdock.server;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/servers/{serverId}/console")
@RequiredArgsConstructor
public class ServerConsoleController {

    private final ServerConsoleService serverConsoleService;

    @GetMapping(path = "/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter logs(@AuthenticationPrincipal Jwt jwt,
                           @PathVariable Long serverId) {
        return serverConsoleService.streamLogs(userId(jwt), serverId);
    }

    @PostMapping("/commands")
    public ConsoleCommandResponse execute(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable Long serverId,
                                          @Valid @RequestBody ConsoleCommandRequest request) {
        return serverConsoleService.execute(userId(jwt), serverId, request);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
