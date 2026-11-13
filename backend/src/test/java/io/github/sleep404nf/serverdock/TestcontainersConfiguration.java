package io.github.sleep404nf.serverdock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mariadb.MariaDBContainer;

/**
 * 테스트용 DB. 테스트를 돌릴 때마다 빈 MariaDB 컨테이너를 띄우고, Spring이 그 주소로 접속한다.
 * 개발용 DB(3307)는 건드리지 않는다. Docker(OrbStack)가 켜져 있어야 한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MariaDBContainer mariaDbContainer() {
        return new MariaDBContainer("mariadb:11.8");
    }
}
