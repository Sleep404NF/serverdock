package io.github.sleep404nf.serverdock.discord;

import io.github.sleep404nf.serverdock.common.BusinessException;
import io.github.sleep404nf.serverdock.docker.ServerStatus;
import io.github.sleep404nf.serverdock.server.ServerPowerService;
import io.github.sleep404nf.serverdock.server.ServerResponse;
import io.github.sleep404nf.serverdock.server.ServerService;
import io.github.sleep404nf.serverdock.server.ServerStatusResponse;
import io.github.sleep404nf.serverdock.user.User;
import io.github.sleep404nf.serverdock.user.UserRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * /server 명령어. 웹과 같은 서비스(권한 검사 포함)를 그대로 부른다.
 * 디스코드 사용자는 웹에서 연결해 둔 ServerDock 계정으로 판단한다.
 *
 * 디스코드는 3초 안에 응답을 요구하므로 먼저 "생각 중" 상태로 답(defer)한 뒤,
 * 실제 처리는 별도 스레드에서 하고 답을 고친다. 서버 켜기는 이미지 내려받기 때문에 오래 걸릴 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServerCommandListener extends ListenerAdapter {

    private static final String COMMAND = "server";
    private static final String NAME_OPTION = "name";
    private static final int MAX_CHOICES = 25;

    private final DiscordProperties properties;
    private final UserRepository userRepository;
    private final ServerService serverService;
    private final ServerPowerService serverPowerService;
    private final DiscordChannelService discordChannelService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    static SlashCommandData definition() {
        return Commands.slash(COMMAND, "마인크래프트 서버 관리 (ServerDock)")
                .addSubcommands(
                        new SubcommandData("list", "내가 속한 서버 목록"),
                        withName(new SubcommandData("status", "서버 상태 보기")),
                        withName(new SubcommandData("start", "서버 켜기")),
                        withName(new SubcommandData("stop", "서버 끄기 (ADMIN 이상)")),
                        withName(new SubcommandData("notify", "이 채널로 켜짐·꺼짐 알림 받기 (ADMIN 이상)")),
                        withName(new SubcommandData("unnotify", "켜짐·꺼짐 알림 끄기 (ADMIN 이상)")));
    }

    private static SubcommandData withName(SubcommandData subcommand) {
        return subcommand.addOption(OptionType.STRING, NAME_OPTION, "서버 이름", true, true);
    }

    @PreDestroy
    void shutdownExecutor() {
        executor.shutdown();
    }

    // ---- 명령어 등록: 길드마다 등록하면 바로 쓸 수 있다 (전체 등록은 퍼지는 데 시간이 걸릴 수 있음) ----

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        registerCommands(event.getGuild());
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        registerCommands(event.getGuild());
    }

    private void registerCommands(Guild guild) {
        guild.updateCommands().addCommands(definition()).queue(
                commands -> log.info("디스코드 명령어를 등록했습니다: {}", guild.getName()),
                error -> log.warn("디스코드 명령어를 등록하지 못했습니다: {}", guild.getName(), error));
    }

    // ---- 명령어 처리 ----

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!COMMAND.equals(event.getName())) {
            return;
        }
        String subcommand = event.getSubcommandName();
        String discordUserId = event.getUser().getId();
        String serverName = event.getOption(NAME_OPTION, OptionMapping::getAsString);
        String guildId = event.getGuild() == null ? null : event.getGuild().getId();
        String channelId = event.getChannel().getId();

        // 목록은 나만 보이게, 나머지는 채널 사람들도 보이게 답한다.
        event.deferReply("list".equals(subcommand)).queue();
        InteractionHook hook = event.getHook();
        executor.submit(() -> hook.editOriginal(handle(subcommand, discordUserId, serverName, guildId, channelId))
                .queue(null, error -> log.warn("디스코드 응답을 보내지 못했습니다.", error)));
    }

    private String handle(String subcommand, String discordUserId, String serverName, String guildId, String channelId) {
        try {
            User user = userRepository.findByDiscordUserId(discordUserId).orElse(null);
            if (user == null) {
                return "먼저 웹에서 디스코드 계정을 연결해주세요: " + properties.accountPageUrl();
            }
            Long userId = user.getId();
            if ("list".equals(subcommand)) {
                return list(userId);
            }

            ServerResponse server = findServer(userId, serverName);
            return switch (subcommand) {
                case "status" -> status(server, serverPowerService.status(userId, server.id()));
                case "start" -> start(userId, server);
                case "stop" -> {
                    serverPowerService.stop(userId, server.id());
                    yield "⚪ **" + server.name() + "** 서버를 껐습니다.";
                }
                case "notify" -> {
                    if (guildId == null) {
                        yield "알림은 디스코드 서버(길드)의 채널에서만 받을 수 있습니다.";
                    }
                    discordChannelService.link(userId, server.id(), guildId, channelId);
                    yield "🔔 이제 이 채널로 **" + server.name() + "** 서버가 켜지고 꺼질 때 알려드립니다.";
                }
                case "unnotify" -> {
                    discordChannelService.unlink(userId, server.id());
                    yield "🔕 **" + server.name() + "** 서버 알림을 껐습니다.";
                }
                default -> "알 수 없는 명령어입니다.";
            };
        } catch (BusinessException e) {
            return "⚠️ " + e.getMessage();
        } catch (RuntimeException e) {
            log.error("디스코드 명령어 처리 중 오류: /server {}", subcommand, e);
            return "⚠️ 처리 중 오류가 발생했습니다. 잠시 뒤 다시 시도해주세요.";
        }
    }

    private String list(Long userId) {
        List<ServerResponse> servers = serverService.findMyServers(userId);
        if (servers.isEmpty()) {
            return "아직 속한 서버가 없습니다. 친구에게 초대 링크를 받아 참가해주세요.";
        }
        StringBuilder message = new StringBuilder("**내 서버**\n");
        for (ServerResponse server : servers) {
            message.append("- ").append(server.name())
                    .append(" (").append(server.gameVersion()).append(' ').append(server.loader())
                    .append(", 포트 ").append(server.port())
                    .append(", ").append(server.myRole()).append(")\n");
        }
        return message.toString();
    }

    private String start(Long userId, ServerResponse server) {
        ServerStatusResponse before = serverPowerService.status(userId, server.id());
        if (before.status() == ServerStatus.RUNNING || before.status() == ServerStatus.STARTING) {
            return status(server, before);
        }
        serverPowerService.start(userId, server.id());
        return "⏳ **" + server.name() + "** 서버를 켜는 중입니다. 보통 1분 안에 켜집니다. (포트 " + server.port() + ")";
    }

    static String status(ServerResponse server, ServerStatusResponse status) {
        String label = switch (status.status()) {
            case RUNNING -> "🟢 켜짐";
            case STARTING -> "🟡 켜는 중";
            case STOPPED -> "⚪ 꺼짐";
            case NOT_CREATED -> "⚪ 아직 한 번도 안 켬";
        };
        String message = "**" + server.name() + "** " + label
                + " · " + server.gameVersion() + " " + server.loader() + " · 포트 " + server.port();
        if (status.restartRequired()) {
            message += "\n설정이 바뀌었습니다. 재시작하면 적용됩니다.";
        }
        return message;
    }

    private ServerResponse findServer(Long userId, String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "서버 이름을 입력해주세요.");
        }
        return serverService.findMyServers(userId).stream()
                .filter(server -> server.name().equalsIgnoreCase(name.strip()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "'" + name + "' 서버를 찾을 수 없습니다. /server list로 이름을 확인해주세요."));
    }

    // ---- 서버 이름 자동완성: 내가 속한 서버만 보여준다 ----

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!COMMAND.equals(event.getName()) || !NAME_OPTION.equals(event.getFocusedOption().getName())) {
            return;
        }
        String typed = event.getFocusedOption().getValue().toLowerCase(Locale.ROOT);
        List<Command.Choice> choices = userRepository.findByDiscordUserId(event.getUser().getId())
                .map(user -> serverService.findMyServers(user.getId()).stream()
                        .map(ServerResponse::name)
                        .filter(name -> name.toLowerCase(Locale.ROOT).contains(typed))
                        .limit(MAX_CHOICES)
                        .map(name -> new Command.Choice(name, name))
                        .toList())
                .orElse(List.of());
        event.replyChoices(choices).queue();
    }
}
