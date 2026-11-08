package io.github.sleep404nf.serverdock.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServerMemberRepository extends JpaRepository<ServerMember, ServerMemberId> {

    @Query("select m from ServerMember m join fetch m.server where m.user.id = :userId order by m.server.id")
    List<ServerMember> findAllByUserIdWithServer(@Param("userId") Long userId);

    /**
     * 서버를 지우기 전에 멤버를 한 번의 쿼리로 지운다.
     */
    @Modifying(clearAutomatically = true)
    @Query("delete from ServerMember m where m.server.id = :serverId")
    void deleteAllByServerId(@Param("serverId") Long serverId);
}
