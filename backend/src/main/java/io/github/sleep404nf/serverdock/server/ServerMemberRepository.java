package io.github.sleep404nf.serverdock.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServerMemberRepository extends JpaRepository<ServerMember, ServerMemberId> {

    @Query("select m from ServerMember m join fetch m.server where m.user.id = :userId order by m.server.id")
    List<ServerMember> findAllByUserIdWithServer(@Param("userId") Long userId);
}
