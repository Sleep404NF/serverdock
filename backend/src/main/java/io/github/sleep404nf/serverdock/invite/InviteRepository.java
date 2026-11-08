package io.github.sleep404nf.serverdock.invite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, Long> {

    Optional<Invite> findByCode(String code);

    Optional<Invite> findByIdAndServerId(Long id, Long serverId);

    @Query("select i from Invite i join fetch i.createdBy where i.server.id = :serverId order by i.id desc")
    List<Invite> findAllByServerIdWithCreator(@Param("serverId") Long serverId);

    /**
     * 사용 횟수를 1 올리고, 이미 한도에 닿았으면 바꾸지않고 0을 반환한다.
     */
    @Modifying
    @Query("update Invite i set i.usedCount = i.usedCount + 1 " +
            "where i.id = :id and (i.maxUses is null or i.usedCount < i.maxUses)")
    int incrementUsedCount(@Param("id") Long id);
}
