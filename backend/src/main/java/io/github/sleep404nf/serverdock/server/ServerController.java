package io.github.sleep404nf.serverdock.server;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServerResponse create(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody CreateServerRequest request) {
        return serverService.create(userId(jwt), request);
    }

    @GetMapping
    public List<ServerResponse> findMyServers(@AuthenticationPrincipal Jwt jwt) {
        return serverService.findMyServers(userId(jwt));
    }

    @GetMapping("/{serverId}")
    public ServerResponse findOne(@AuthenticationPrincipal Jwt jwt,
                                  @PathVariable Long serverId) {
        return serverService.findOne(userId(jwt), serverId);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
