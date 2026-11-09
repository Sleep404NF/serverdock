package io.github.sleep404nf.serverdock.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServerMemberRepository extends JpaRepository<ServerMember, ServerMemberId> {

    /**
     * 내가 속한 서버 목록. 휴지통에 있는 서버는 빼고 가져온다.
     */
    @Query("select m from ServerMember m join fetch m.server " +
            "where m.user.id = :userId and m.server.deletedAt is null order by m.server.id")
    List<ServerMember> findAllByUserIdWithServer(@Param("userId") Long userId);

    /**
     * 휴지통에 있는 서버 중 내가 해당 역할인 것. 최근에 지운 것부터.
     */
    @Query("select m from ServerMember m join fetch m.server " +
            "where m.user.id = :userId and m.role = :role and m.server.deletedAt is not null " +
            "order by m.server.deletedAt desc")
    List<ServerMember> findAllInTrashByUserIdAndRole(@Param("userId") Long userId, @Param("role") ServerRole role);

    /**
     * 서버의 멤버 목록을 사용자 정보와 함께 한 번에 가져온다.
     */
    @Query("select m from ServerMember m join fetch m.user where m.server.id = :serverId")
    List<ServerMember> findAllByServerIdWithUser(@Param("serverId") Long serverId);

    /**
     * 서버를 지우기 전에 멤버를 한 번의 쿼리로 지운다.
     */
    @Modifying(clearAutomatically = true)
    @Query("delete from ServerMember m where m.server.id = :serverId")
    void deleteAllByServerId(@Param("serverId") Long serverId);
}
