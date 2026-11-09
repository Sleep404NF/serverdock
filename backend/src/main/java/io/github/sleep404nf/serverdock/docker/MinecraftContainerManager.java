package io.github.sleep404nf.serverdock.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import io.github.sleep404nf.serverdock.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 마인크래프트 서버 컨테이너를 만들고, 켜고, 끄고, 상태를 읽는다.
 * 컨테이너는 이름(serverdock-server-{id})으로 찾고, 월드는 serverdock-server-{id}-data 볼륨에 저장한다.
 * 컨테이너에는 설정 지문(config-hash) 라벨을 붙여서, 설정이 바뀌면 다음 시작 때 컨테이너를 다시 만든다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinecraftContainerManager {

    private static final int MC_PORT = 25565;
    private static final int STOP_TIMEOUT_SECONDS = 30;
    private static final long PULL_TIMEOUT_MINUTES = 10;
    private static final String LABEL_SERVER_ID = "serverdock.server-id";
    private static final String LABEL_CONFIG_HASH = "serverdock.config-hash";

    private final DockerClient dockerClient;
    private final DockerProperties dockerProperties;

    /**
     * 서버를 켠다. 컨테이너가 없거나 설정이 바뀌었으면 새로 만들고, 이미 켜져 있으면 그대로 둔다.
     * 켜져 있는 동안 바뀐 설정은 재시작할 때 적용된다. 컨테이너 id를 돌려준다.
     */
    public String start(MinecraftServerSpec spec) {
        Optional<InspectContainerResponse> existing = findContainer(spec.serverId());

        if (existing.isPresent() && isRunning(existing.get())) {
            return existing.get().getId();
        }

        String desiredHash = configHash(spec);
        String containerId;
        if (existing.isPresent() && desiredHash.equals(configHashOf(existing.get()))) {
            containerId = existing.get().getId();
        } else {
            existing.ifPresent(old -> removeContainer(old.getId()));
            containerId = create(spec, desiredHash);
        }

        startContainer(containerId, spec.port());
        return containerId;
    }

    /**
     * 서버를 껐다가 다시 켠다. 바뀐 설정이 있으면 이때 컨테이너를 다시 만든다.
     */
    public String restart(MinecraftServerSpec spec) {
        stop(spec.serverId());
        return start(spec);
    }

    /**
     * 서버를 끈다. 마인크래프트가 월드를 저장할 시간을 30초 준다.
     */
    public void stop(long serverId) {
        try {
            dockerClient.stopContainerCmd(containerName(serverId)).withTimeout(STOP_TIMEOUT_SECONDS).exec();
        } catch (NotModifiedException | NotFoundException e) {
            log.debug("이미 꺼져 있거나 없는 컨테이너입니다: 서버 {}", serverId);
        }
    }

    /**
     * 서버 상태. 마인크래프트 서버가 아직 뜨는 중이면 STARTING이다.
     */
    public ServerStatus status(long serverId) {
        Optional<InspectContainerResponse> container = findContainer(serverId);
        if (container.isEmpty()) {
            return ServerStatus.NOT_CREATED;
        }

        InspectContainerResponse.ContainerState state = container.get().getState();
        if (!Boolean.TRUE.equals(state.getRunning())) {
            return ServerStatus.STOPPED;
        }
        if (state.getHealth() != null && "starting".equals(state.getHealth().getStatus())) {
            return ServerStatus.STARTING;
        }
        return ServerStatus.RUNNING;
    }

    /**
     * 컨테이너가 만들어진 뒤로 설정이 바뀌었는지. true면 재시작해야 바뀐 설정이 적용된다.
     */
    public boolean isOutdated(MinecraftServerSpec spec) {
        return findContainer(spec.serverId())
                .map(container -> !configHash(spec).equals(configHashOf(container)))
                .orElse(false);
    }

    /**
     * 컨테이너와 월드 볼륨을 모두 지운다. 되돌릴 수 없다. 휴지통 보관 기간이 끝난 서버에만 쓴다.
     */
    public void removeServer(long serverId) {
        try {
            dockerClient.removeContainerCmd(containerName(serverId)).withForce(true).exec();
        } catch (NotFoundException e) {
            log.debug("이미 없는 컨테이너입니다: 서버 {}", serverId);
        }
        try {
            dockerClient.removeVolumeCmd(volumeName(serverId)).exec();
        } catch (NotFoundException e) {
            log.debug("이미 없는 볼륨입니다: 서버 {}", serverId);
        }
        log.info("서버 {}의 컨테이너와 월드를 지웠습니다.", serverId);
    }

    private String create(MinecraftServerSpec spec, String configHash) {
        pullImageIfMissing();

        ExposedPort gamePort = ExposedPort.tcp(MC_PORT);
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withPortBindings(new PortBinding(Ports.Binding.bindPort(spec.port()), gamePort))
                .withBinds(new Bind(volumeName(spec.serverId()), new Volume("/data")));

        CreateContainerResponse response = dockerClient.createContainerCmd(dockerProperties.image())
                .withName(containerName(spec.serverId()))
                .withEnv(environment(spec))
                .withExposedPorts(gamePort)
                .withHostConfig(hostConfig)
                .withLabels(Map.of(
                        LABEL_SERVER_ID, String.valueOf(spec.serverId()),
                        LABEL_CONFIG_HASH, configHash
                ))
                .exec();

        log.info("컨테이너를 만들었습니다: {} (서버 {}, 설정 {})", response.getId(), spec.serverId(), configHash);
        return response.getId();
    }

    private void startContainer(String containerId, int port) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
        } catch (DockerException e) {
            if (isPortConflict(e)) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "포트 " + port + "를 이미 다른 프로그램이 쓰고 있습니다.");
            }
            throw e;
        }
    }

    /**
     * 컨테이너만 지운다. 월드가 든 볼륨은 이름이 붙은 볼륨이라 같이 지워지지 않는다.
     */
    private void removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            log.info("설정이 바뀌어 컨테이너를 지웠습니다: {}", containerId);
        } catch (NotFoundException e) {
            log.debug("이미 없는 컨테이너입니다: {}", containerId);
        }
    }

    private Optional<InspectContainerResponse> findContainer(long serverId) {
        try {
            return Optional.of(dockerClient.inspectContainerCmd(containerName(serverId)).exec());
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    private boolean isRunning(InspectContainerResponse container) {
        return Boolean.TRUE.equals(container.getState().getRunning());
    }

    private String configHashOf(InspectContainerResponse container) {
        Map<String, String> labels = container.getConfig().getLabels();
        return labels == null ? null : labels.get(LABEL_CONFIG_HASH);
    }

    /**
     * 이미지, 포트, 환경변수를 합친 설정 지문. 이 중 하나라도 바뀌면 값이 달라진다.
     */
    private String configHash(MinecraftServerSpec spec) {
        String source = dockerProperties.image() + "|" + spec.port() + "|" + String.join(",", environment(spec));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    /**
     * 이미지가 없으면 내려받는다. 저장소와 태그를 나눠야 모든 태그를 받아오는 사고가 나지 않는다.
     */
    private void pullImageIfMissing() {
        String image = dockerProperties.image();
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (NotFoundException e) {
            log.info("이미지를 내려받습니다: {}", image);
            int tagIndex = image.lastIndexOf(':');
            boolean hasTag = tagIndex > image.lastIndexOf('/');
            String repository = hasTag ? image.substring(0, tagIndex) : image;
            String tag = hasTag ? image.substring(tagIndex + 1) : "latest";
            try {
                dockerClient.pullImageCmd(repository)
                        .withTag(tag)
                        .start()
                        .awaitCompletion(PULL_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("이미지를 내려받는 중에 중단됐습니다: " + image, interrupted);
            }
        }
    }

    /**
     * itzg/minecraft-server 이미지가 읽는 환경변수. 시간대는 백엔드가 도는 컴퓨터를 따른다.
     */
    private List<String> environment(MinecraftServerSpec spec) {
        List<String> env = new ArrayList<>(List.of(
                "EULA=TRUE",
                "TYPE=" + spec.type(),
                "VERSION=" + spec.gameVersion(),
                "MEMORY=" + spec.memoryMb() + "M",
                "TZ=" + ZoneId.systemDefault().getId()
        ));

        if (spec.loaderVersion() != null) {
            switch (spec.type()) {
                case "FABRIC" -> env.add("FABRIC_LOADER_VERSION=" + spec.loaderVersion());
                case "FORGE" -> env.add("FORGE_VERSION=" + spec.loaderVersion());
                case "NEOFORGE" -> env.add("NEOFORGE_VERSION=" + spec.loaderVersion());
                default -> {
                }
            }
        }
        return env;
    }

    private boolean isPortConflict(DockerException e) {
        String message = String.valueOf(e.getMessage());
        return message.contains("port is already allocated") || message.contains("address already in use");
    }

    static String containerName(long serverId) {
        return "serverdock-server-" + serverId;
    }

    private static String volumeName(long serverId) {
        return "serverdock-server-" + serverId + "-data";
    }
}
