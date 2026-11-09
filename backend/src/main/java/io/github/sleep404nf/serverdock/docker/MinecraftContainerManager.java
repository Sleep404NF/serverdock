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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 마인크래프트 서버 컨테이너를 만들고, 켜고, 끄고, 상태를 읽는다.
 * 컨테이너 이름은 serverdock-server-{id}, 월드는 serverdock-server-{id}-data 볼륨에 저장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinecraftContainerManager {

    private static final int MC_PORT = 25565;
    private static final int STOP_TIMEOUT_SECONDS = 30;
    private static final long PULL_TIMEOUT_MINUTES = 10;

    private final DockerClient dockerClient;
    private final DockerProperties dockerProperties;

    /**
     * 컨테이너가 없으면 만들고, 꺼져 있으면 켠다. 컨테이너 id를 돌려준다.
     */
    public String start(MinecraftServerSpec spec) {
        String containerId = findContainerId(spec.serverId()).orElseGet(() -> create(spec));

        if (!isRunning(containerId)) {
            try {
                dockerClient.startContainerCmd(containerId).exec();
            } catch (DockerException e) {
                if (isPortConflict(e)) {
                    throw new BusinessException(HttpStatus.CONFLICT,
                            "포트 " + spec.port() + "를 이미 다른 프로그램이 쓰고 있습니다.");
                }
                throw e;
            }
        }
        return containerId;
    }

    /**
     * 컨테이너를 멈춘다. 마인크래프트가 월드를 저장할 시간을 30초 준다.
     */
    public void stop(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).withTimeout(STOP_TIMEOUT_SECONDS).exec();
        } catch (NotModifiedException | NotFoundException e) {
            log.debug("이미 꺼져 있거나 없는 컨테이너입니다: {}", containerId);
        }
    }

    /**
     * 컨테이너 상태. 마인크래프트 서버가 아직 뜨는 중이면 STARTING이다.
     */
    public ServerStatus status(String containerId) {
        if (containerId == null) {
            return ServerStatus.NOT_CREATED;
        }
        try {
            InspectContainerResponse.ContainerState state =
                    dockerClient.inspectContainerCmd(containerId).exec().getState();

            if (!Boolean.TRUE.equals(state.getRunning())) {
                return ServerStatus.STOPPED;
            }
            if (state.getHealth() != null && "starting".equals(state.getHealth().getStatus())) {
                return ServerStatus.STARTING;
            }
            return ServerStatus.RUNNING;
        } catch (NotFoundException e) {
            return ServerStatus.NOT_CREATED;
        }
    }

    private String create(MinecraftServerSpec spec) {
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
                .withLabels(Map.of("serverdock.server-id", String.valueOf(spec.serverId())))
                .exec();

        log.info("컨테이너를 만들었습니다: {} (서버 {})", response.getId(), spec.serverId());
        return response.getId();
    }

    private Optional<String> findContainerId(long serverId) {
        try {
            return Optional.of(dockerClient.inspectContainerCmd(containerName(serverId)).exec().getId());
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    private boolean isRunning(String containerId) {
        return Boolean.TRUE.equals(dockerClient.inspectContainerCmd(containerId).exec().getState().getRunning());
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
     * itzg/minecraft-server 이미지가 읽는 환경변수
     */
    private List<String> environment(MinecraftServerSpec spec) {
        List<String> env = new ArrayList<>(List.of(
                "EULA=TRUE",
                "TYPE=" + spec.type(),
                "VERSION=" + spec.gameVersion(),
                "MEMORY=" + spec.memoryMb() + "M"
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

    private static String containerName(long serverId) {
        return "serverdock-server-" + serverId;
    }

    private static String volumeName(long serverId) {
        return "serverdock-server-" + serverId + "-data";
    }
}