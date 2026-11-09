package io.github.sleep404nf.serverdock.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ServerRepository extends JpaRepository<Server, Long> {

    boolean existsByPort(int port);

    /**
     * DB 콜레이션(utf8mb4_unicode_ci) 때문에 대소문자를 구분하지 않고 비교된다.
     */
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("select s.port from Server s")
    List<Integer> findAllPorts();

    /**
     * 휴지통에 들어간 지 threshold보다 오래된 서버. 완전 삭제 대상이다.
     */
    @Query("select s from Server s where s.deletedAt is not null and s.deletedAt < :threshold")
    List<Server> findAllInTrashDeletedBefore(@Param("threshold") LocalDateTime threshold);
}
