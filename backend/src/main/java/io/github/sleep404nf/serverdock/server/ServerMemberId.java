package io.github.sleep404nf.serverdock.server;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * server_members의 복합 키 (server_id, user_id)
 */
@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServerMemberId implements Serializable {

    @Column(name = "server_id")
    private Long serverId;

    @Column(name = "user_id")
    private Long userId;
}
