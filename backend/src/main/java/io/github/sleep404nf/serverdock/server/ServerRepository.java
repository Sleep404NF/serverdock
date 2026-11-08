package io.github.sleep404nf.serverdock.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
