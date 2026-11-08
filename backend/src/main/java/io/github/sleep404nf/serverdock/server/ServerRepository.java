package io.github.sleep404nf.serverdock.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServerRepository extends JpaRepository<Server, Long> {

    boolean existsByPort(int port);

    @Query("select s.port from Server s")
    List<Integer> findAllPorts();
}
