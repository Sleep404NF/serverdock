package io.github.sleep404nf.serverdock.invite;

import java.time.Duration;

/**
 * 초대 링크 만료 시간 선택지
 */
public enum InviteExpiry {
    THIRTY_MINUTES(Duration.ofMinutes(30)),
    ONE_HOUR(Duration.ofHours(1)),
    SIX_HOURS(Duration.ofHours(6)),
    TWELVE_HOURS(Duration.ofHours(12)),
    ONE_DAY(Duration.ofDays(1)),
    SEVEN_DAYS(Duration.ofDays(7));

    private final Duration duration;

    InviteExpiry(Duration duration) {
        this.duration = duration;
    }

    public Duration duration() {
        return duration;
    }
}
